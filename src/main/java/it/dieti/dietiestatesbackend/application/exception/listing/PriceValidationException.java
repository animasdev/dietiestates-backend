package it.dieti.dietiestatesbackend.application.exception.listing;

import it.dieti.dietiestatesbackend.application.exception.ApplicationHttpException;
import java.util.List;
import org.springframework.http.HttpStatus;

public class PriceValidationException extends ApplicationHttpException {

    private PriceValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message, List.of(new FieldErrorDetail("price_cents", message)));
    }

    public static PriceValidationException required() {
        return new PriceValidationException("Il campo 'priceCents' è obbligatorio.");
    }

    public static PriceValidationException mustBePositive() {
        return new PriceValidationException("Il campo 'priceCents' deve essere maggiore o uguale a zero.");
    }
}
