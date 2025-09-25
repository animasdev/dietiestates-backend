package it.dieti.dietiestatesbackend.application.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void sendSignUpConfirmation(String email, String token) {
        log.info("Sign-up confirmation requested for email={}", email);
        log.info("Use token to confirm: {}", token);
    }
}
