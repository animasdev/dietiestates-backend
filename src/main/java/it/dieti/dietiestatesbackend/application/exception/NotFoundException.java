package it.dieti.dietiestatesbackend.application.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApplicationHttpException {

    private NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public static NotFoundException resourceNotFound(String resourceName, Object resourceId) {
        var normalizedResource = resourceName == null || resourceName.isBlank() ? "Risorsa" : resourceName;
        var normalizedId = resourceId == null ? "" : " " + resourceId;
        return new NotFoundException(normalizedResource + normalizedId + " non trovata.");
    }
}
