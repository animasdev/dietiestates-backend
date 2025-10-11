package it.dieti.dietiestatesbackend.application.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApplicationHttpException {

    private ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    public static ConflictException of(String message) {
        return new ConflictException(message);
    }
}
