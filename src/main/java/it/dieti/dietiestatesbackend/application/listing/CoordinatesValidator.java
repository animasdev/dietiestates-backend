package it.dieti.dietiestatesbackend.application.listing;

import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class CoordinatesValidator {

    public void validate(double latitude, double longitude) {
        if (Double.isNaN(latitude)) {
            throw BadRequestException.forField("lat", "Il parametro 'lat' deve essere un numero valido.");
        }
        if (Double.isNaN(longitude)) {
            throw BadRequestException.forField("lng", "Il parametro 'lng' deve essere un numero valido.");
        }
        if (latitude < -90 || latitude > 90) {
            throw BadRequestException.forField("lat", "Il parametro 'lat' deve essere compreso tra -90 e 90.");
        }
        if (longitude < -180 || longitude > 180) {
            throw BadRequestException.forField("lng", "Il parametro 'lng' deve essere compreso tra -180 e 180.");
        }
    }
}

