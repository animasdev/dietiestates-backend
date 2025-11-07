package it.dieti.dietiestatesbackend.application.notification;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationProperties properties;
    private final EmailDispatchService dispatchService;

    public NotificationService(EmailDispatchService dispatchService, NotificationProperties properties) {
        this.dispatchService = dispatchService;
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
        dispatch(email, subject, body);
    }

    public void sendPasswordReset(String email, String token) {
        var subject = "Reset password DietiEstates";
        var body = """
                Hai richiesto il reset della password.

                Usa questo codice per impostarne una nuova: %s

                Se non hai richiesto tu il reset, contatta il supporto.

                -- Team DietiEstates
                """.formatted(token);
        dispatch(email, subject, body);
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
        dispatch(agentEmail, subject, bodyBuilder.toString());
    }

    public void sendMoveToDraft(String agentEmail, String listingTitle, UUID listingId, @Nullable String reason) {
        var subject = "Annuncio spostato in DRAFT: \"" + listingTitle + "\"";
        var bodyBuilder = new StringBuilder()
                .append("L'annuncio \"").append(listingTitle)
                .append("\" (ID ").append(listingId)
                .append(") è stato riportato allo stato DRAFT da un amministratore.\n\n");

        if (StringUtils.hasText(reason)) {
            bodyBuilder.append("Motivazione: ").append(reason).append("\n\n");
        }

        bodyBuilder.append("Puoi modificare l'annuncio e ripubblicarlo quando pronto.\n\n-- Team DietiEstates");
        dispatch(agentEmail, subject, bodyBuilder.toString());
    }

    private void dispatch(String recipient, String subject, String text) {
        if (properties.isAsyncEnabled()) {
            dispatchService.sendEmailAsync(recipient, subject, text);
        } else {
            dispatchService.sendEmailSync(recipient, subject, text);
        }
    }
}
