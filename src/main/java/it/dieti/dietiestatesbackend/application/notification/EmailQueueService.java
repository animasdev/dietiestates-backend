package it.dieti.dietiestatesbackend.application.notification;

import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.notification.EmailNotificationEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.notification.EmailNotificationJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static it.dieti.dietiestatesbackend.application.notification.EmailStatus.*;

@Service
public class EmailQueueService {
    private static final Logger log = LoggerFactory.getLogger(EmailQueueService.class);

    private final NotificationProperties properties;
    private final EmailNotificationJpaRepository repo;
    @Nullable
    private final JavaMailSender mailSender;

    public EmailQueueService(NotificationProperties properties,
                             EmailNotificationJpaRepository repo,
                             @Nullable JavaMailSender mailSender) {
        this.properties = properties;
        this.repo = repo;
        this.mailSender = mailSender;
    }

    /**
     * Enqueue an email for delivery, returning the persisted id.
     */
    @Transactional
    public UUID enqueue(String recipient, String subject, String body) {
        var now = OffsetDateTime.now();
        var e = new EmailNotificationEntity();
        e.setRecipient(recipient);
        e.setSubject(subject);
        e.setBody(body);
        e.setBodyHash(DigestUtils.md5DigestAsHex(body.getBytes(StandardCharsets.UTF_8)));
        e.setStatus(QUEUED);
        e.setAttempts(0);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        var saved = repo.save(e);
        return saved.getId();
    }

    /**
     * Optionally process the queued email immediately via @Async, to minimize latency.
     */
    @Async("notificationExecutor")
    public void processQueuedAsync(UUID id) {
        processOne(id, true);
    }

    /**
     * Scheduler that periodically scans for pending emails and processes them.
     */
    @Scheduled(fixedDelayString = "${app.notification.schedulerFixedDelayMillis:15000}")
    public void scheduledRetry() {
        if (!properties.isSchedulerEnabled()) {
            return;
        }
        var pending = repo.findPending(List.of(QUEUED, RETRYING));
        for (var e : pending) {
            // honor backoff without storing it in DB; skip if not yet due
            if (!isDueForRetry(e)) {
                continue;
            }
            try {
                processOne(e.getId(), false);
            } catch (Exception ex) {
                log.error("Scheduler failed processing email {}", e.getId(), ex);
            }
        }
    }

    private boolean isDueForRetry(EmailNotificationEntity e) {
        int attempts = e.getAttempts();
        if (attempts == 0) return true; // first try ASAP
        long delay = computeBackoffMillis(attempts);
        var nextTime = e.getUpdatedAt().plusNanos(delay * 1_000_000);
        return OffsetDateTime.now().isAfter(nextTime);
    }

    private long computeBackoffMillis(int attemptNumber) {
        // attemptNumber starts at 0; for retries we consider the attempt already made
        long base = properties.getBackoffBaseMillis();
        double mult = properties.getBackoffMultiplier();
        // For attempt 1 => base, 2 => base*mult, etc.
        long delay = (long) (base * Math.pow(mult, Math.max(0, attemptNumber - 1)));
        return Math.max(base, delay);
    }

    @Transactional
    public void processOne(UUID id, boolean allowEscalation) {
        var e = repo.findById(id).orElse(null);
        if (e == null) return;
        if (!properties.isEnabled() || mailSender == null) {
            log.info("Email sending disabled or mailSender missing. Keeping queued. id={} to={} subject={}",
                    e.getId(), e.getRecipient(), e.getSubject());
            return;
        }
        if (!StringUtils.hasText(properties.getFromEmail())) {
            log.warn("fromEmail not configured; cannot send emails. Keeping queued. id={} to={} subject={}",
                    e.getId(), e.getRecipient(), e.getSubject());
            return;
        }

        int maxAttempts = Math.max(1, properties.getMaxAttempts());
        while (e.getAttempts() < maxAttempts && e.getStatus() != SENT) {
            try {
                e.setStatus(RETRYING);
                e.setUpdatedAt(OffsetDateTime.now());
                repo.save(e);

                var msg = new SimpleMailMessage();
                msg.setFrom(properties.formattedFromAddress());
                msg.setTo(e.getRecipient());
                msg.setSubject(e.getSubject());
                msg.setText(e.getBody());
                mailSender.send(msg);

                e.setStatus(SENT);
                e.setSentAt(OffsetDateTime.now());
                e.setUpdatedAt(OffsetDateTime.now());
                e.setLastError(null);
                repo.save(e);
                log.info("Email SENT id={} to={} subject={}", e.getId(), e.getRecipient(), e.getSubject());
                break;
            } catch (MailException ex) {
                e.setAttempts(e.getAttempts() + 1);
                e.setLastError(truncateError(ex));
                e.setUpdatedAt(OffsetDateTime.now());
                if (e.getAttempts() >= maxAttempts) {
                    e.setStatus(FAILED);
                    repo.save(e);
                    log.error("Email FAILED after {} attempts. id={} to={} subject={}",
                            e.getAttempts(), e.getId(), e.getRecipient(), e.getSubject(), ex);
                    if (allowEscalation) {
                        escalateFailure(e);
                    }
                    break;
                } else {
                    e.setStatus(RETRYING);
                    repo.save(e);
                    long sleepMs = computeBackoffMillis(e.getAttempts());
                    try { Thread.sleep(sleepMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
    }

    private void escalateFailure(EmailNotificationEntity e) {
        var support = properties.getSupportEmail();
        if (!StringUtils.hasText(support)) return;
        // Avoid recursion: do not enqueue; try one-off sync send; do not re-escalate on failure
        try {
            var msg = new SimpleMailMessage();
            msg.setFrom(properties.formattedFromAddress());
            msg.setTo(support);
            msg.setSubject("[ALERT] Email delivery failure: " + e.getSubject());
            msg.setText("Email failed after attempts=" + e.getAttempts() + "\n" +
                    "Recipient: " + e.getRecipient() + "\n" +
                    "Subject: " + e.getSubject() + "\n" +
                    "Last error: " + e.getLastError() + "\n" +
                    "Id: " + e.getId());
            if (mailSender != null) {
                mailSender.send(msg);
                log.warn("Escalation email sent to support {} for failed email {}", support, e.getId());
            }
        } catch (Exception ex) {
            log.error("Failed to send escalation email to support {} for failed email {}", support, e.getId(), ex);
        }
    }

    private static String truncateError(Throwable ex) {
        String msg = ex.getMessage();
        if (msg == null) msg = ex.getClass().getName();
        return msg.length() > 4000 ? msg.substring(0, 4000) : msg;
    }
}

