package it.dieti.dietiestatesbackend.application.exception.listing;

import it.dieti.dietiestatesbackend.application.exception.ApplicationHttpException;
import org.springframework.http.HttpStatus;

public class ListingTypeNotSupportedException extends ApplicationHttpException {

    private static final String BASE_MESSAGE = "Listing type non supportato";

    private final String requestedType;

    public ListingTypeNotSupportedException(String requestedType) {
        super(HttpStatus.BAD_REQUEST, BASE_MESSAGE + (requestedType != null ? ": " + requestedType : ""));
        this.requestedType = requestedType;
    }

    public String requestedType() {
        return requestedType;
    }
}
