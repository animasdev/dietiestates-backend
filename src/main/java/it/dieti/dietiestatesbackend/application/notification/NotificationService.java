package it.dieti.dietiestatesbackend.application.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void sendSignUpConfirmation(String email, String token) {
        log.info("Sign-up confirmation requested for email={}", email);
        log.info("Use token to confirm: {}", token);
    }

    public void sendPasswordReset(String email, String token) {
        log.info("Password reset requested for email={}", email);
        log.info("Use token to reset password: {}", token);
    }

    public void sendDeleteListing(String agentEmail, String listingTitle, UUID listingId, String reason) {
        log.info("Deletion notification for email={}", agentEmail);
        log.info("The listing {} (id {}) is pending for deletion.", listingTitle, listingId);
        if (reason != null) {
            log.info("This action was taken by an admin for the following reason: '{}'", reason);
        }
    }

}
