package it.dieti.dietiestatesbackend.application.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Nullable
    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public NotificationService(@Nullable JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendSignUpConfirmation(String email, String token) {
        var subject = "Conferma registrazione DietiEstates";
        var body = """
                Ciao,

                per completare la registrazione usa il seguente codice di conferma: %s

                Se non hai richiesto tu questa operazione puoi ignorare questa email.

                -- Team DietiEstates
                """.formatted(token);
        sendEmail(email, subject, body);
    }

    public void sendPasswordReset(String email, String token) {
        var subject = "Reset password DietiEstates";
        var body = """
                Hai richiesto il reset della password.

                Usa questo codice per impostarne una nuova: %s

                Se non hai richiesto tu il reset, contatta il supporto.

                -- Team DietiEstates
                """.formatted(token);
        sendEmail(email, subject, body);
    }

    public void sendDeleteListing(String agentEmail, String listingTitle, UUID listingId, @Nullable String reason) {
        var subject = "Richiesta cancellazione annuncio \"" + listingTitle + "\"";
        var bodyBuilder = new StringBuilder()
                .append("L'annuncio \"").append(listingTitle)
                .append("\" (ID ").append(listingId).append(") è stato contrassegnato per la cancellazione entro 24 ore.\n\n");

        if (StringUtils.hasText(reason)) {
            bodyBuilder.append("Motivazione fornita dall'amministratore: ").append(reason).append("\n\n");
        }

        bodyBuilder.append("Se non riconosci la richiesta contatta il supporto al più presto.\n\n-- Team DietiEstates");
        sendEmail(agentEmail, subject, bodyBuilder.toString());
    }

    private void sendEmail(String recipient, String subject, String text) {
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

}
