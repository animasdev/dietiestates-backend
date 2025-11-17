package it.dieti.dietiestatesbackend.application.notification;

import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.notification.EmailNotificationEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.notification.EmailNotificationJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

import static it.dieti.dietiestatesbackend.application.notification.EmailStatus.*;

@Service
public class EmailDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    private final NotificationProperties properties;
    private final EmailNotificationJpaRepository repo;
    @Nullable
    private final JavaMailSender mailSender;

    public EmailDeliveryService(NotificationProperties properties,
                                EmailNotificationJpaRepository repo,
                                @Nullable JavaMailSender mailSender) {
        this.properties = properties;
        this.repo = repo;
        this.mailSender = mailSender;
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
        while (shouldContinue(e, maxAttempts)) {
            try {
                markRetrying(e);
                sendSimpleEmail(e);
                markSent(e);
                log.info("Email SENT id={} to={} subject={}", e.getId(), e.getRecipient(), e.getSubject());
                break;
            } catch (MailException ex) {
                if (handleFailure(e, ex, allowEscalation, maxAttempts)) {
                    break; // give up (FAILED)
                }
                sleepBackoff(e.getAttempts());
            }
        }
    }

    private boolean shouldContinue(EmailNotificationEntity e, int maxAttempts) {
        return e.getAttempts() < maxAttempts && e.getStatus() != SENT;
    }

    private void markRetrying(EmailNotificationEntity e) {
        e.setStatus(RETRYING);
        e.setUpdatedAt(OffsetDateTime.now());
        repo.save(e);
    }

    private void sendSimpleEmail(EmailNotificationEntity e) throws MailException {
        var msg = new SimpleMailMessage();
        msg.setFrom(properties.formattedFromAddress());
        msg.setTo(e.getRecipient());
        msg.setSubject(e.getSubject());
        msg.setText(e.getBody());
        if (mailSender != null) {
            mailSender.send(msg);
        }
    }

    private void markSent(EmailNotificationEntity e) {
        e.setStatus(SENT);
        e.setSentAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        e.setLastError(null);
        repo.save(e);
    }

    /**
     * Handle a failed attempt. Returns true if the flow should stop (FAILED), false if it should retry.
     */
    private boolean handleFailure(EmailNotificationEntity e, MailException ex, boolean allowEscalation, int maxAttempts) {
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
            return true; // stop
        }

        e.setStatus(RETRYING);
        repo.save(e);
        return false; // will retry after backoff sleep
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

    private long computeBackoffMillis(int attemptNumber) {
        long base = properties.getBackoffBaseMillis();
        double mult = properties.getBackoffMultiplier();
        long delay = (long) (base * Math.pow(mult, Math.max(0, attemptNumber - 1)));
        return Math.max(base, delay);
    }

    private void sleepBackoff(int attemptCount) {
        long sleepMs = computeBackoffMillis(attemptCount);
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}

