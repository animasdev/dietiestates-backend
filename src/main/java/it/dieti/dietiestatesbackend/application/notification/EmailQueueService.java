package it.dieti.dietiestatesbackend.application.notification;

import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.notification.EmailNotificationEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.notification.EmailNotificationJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

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
    private final EmailDeliveryService deliveryService;
    @Nullable
    private final JavaMailSender mailSender;

    public EmailQueueService(NotificationProperties properties,
                             EmailNotificationJpaRepository repo,
                             @Nullable JavaMailSender mailSender,
                             EmailDeliveryService deliveryService) {
        this.properties = properties;
        this.repo = repo;
        this.mailSender = mailSender;
        this.deliveryService = deliveryService;
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

    public void processOne(UUID id, boolean allowEscalation) {
        // Delegate to a separate bean so that @Transactional is applied via proxy
        deliveryService.processOne(id, allowEscalation);
    }

    // Helpers related to scheduling/backoff decision remain here for queue scanning
}
