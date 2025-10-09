package it.dieti.dietiestatesbackend.application.exception.listing;

import it.dieti.dietiestatesbackend.application.exception.ApplicationHttpException;
import org.springframework.http.HttpStatus;

public class AgentProfileRequiredException extends ApplicationHttpException {

    private static final String DEFAULT_MESSAGE = "Questa operazione richiede un profilo agente";

    public AgentProfileRequiredException() {
        super(HttpStatus.UNAUTHORIZED, DEFAULT_MESSAGE);
    }
}
