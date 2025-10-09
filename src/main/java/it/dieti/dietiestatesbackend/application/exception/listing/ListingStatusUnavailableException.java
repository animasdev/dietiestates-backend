package it.dieti.dietiestatesbackend.application.exception.listing;

import it.dieti.dietiestatesbackend.application.exception.ApplicationHttpException;
import org.springframework.http.HttpStatus;

public class ListingStatusUnavailableException extends ApplicationHttpException {

    private static final String GENERIC_MESSAGE = "Impossibile completare la creazione dell'annuncio. Riprova più tardi.";

    private final String statusCode;

    public ListingStatusUnavailableException(String statusCode) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_MESSAGE);
        this.statusCode = statusCode;
    }

    public String statusCode() {
        return statusCode;
    }
}
