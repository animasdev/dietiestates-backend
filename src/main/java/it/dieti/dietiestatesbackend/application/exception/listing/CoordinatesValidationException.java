package it.dieti.dietiestatesbackend.application.exception.listing;

import it.dieti.dietiestatesbackend.application.exception.ApplicationHttpException;
import java.util.List;
import org.springframework.http.HttpStatus;

public class CoordinatesValidationException extends ApplicationHttpException {

    private CoordinatesValidationException(String field, String message) {
        super(HttpStatus.BAD_REQUEST, message, List.of(new FieldErrorDetail(field, message)));
    }

    public static CoordinatesValidationException notANumber(String field) {
        return new CoordinatesValidationException(field, "Il campo '" + field + "' deve essere un numero.");
    }

    public static CoordinatesValidationException latitudeOutOfRange() {
        return new CoordinatesValidationException("geo.lat", "La latitudine deve essere compresa tra -90 e 90.");
    }

    public static CoordinatesValidationException longitudeOutOfRange() {
        return new CoordinatesValidationException("geo.lng", "La longitudine deve essere compresa tra -180 e 180.");
    }
}
