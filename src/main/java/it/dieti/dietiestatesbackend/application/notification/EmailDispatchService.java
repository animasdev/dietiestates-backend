package it.dieti.dietiestatesbackend.application.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailDispatchService {
    private static final Logger log = LoggerFactory.getLogger(EmailDispatchService.class);

    @Nullable
    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public EmailDispatchService(@Nullable JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendEmailSync(String recipient, String subject, String text) {
        if (!properties.isEnabled() || mailSender == null) {
            log.info("Email notification skipped (disabled or mail sender missing). to={}, subject={}", recipient, subject);
            return;
        }
        if (!StringUtils.hasText(properties.getFromEmail())) {
            log.warn("Email notification skipped: fromEmail non configurato. to={}, subject={}", recipient, subject);
            return;
        }

        try {
            var message = new SimpleMailMessage();
            message.setFrom(properties.formattedFromAddress());
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Email inviata a {} con subject '{}'", recipient, subject);
        } catch (MailException ex) {
            log.error("Invio email fallito verso {} per subject '{}'", recipient, subject, ex);
        }
    }

    @Async("notificationExecutor")
    public void sendEmailAsync(String recipient, String subject, String text) {
        sendEmailSync(recipient, subject, text);
    }
}

