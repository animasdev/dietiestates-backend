package it.dieti.dietiestatesbackend.application.exception;

import org.springframework.http.HttpStatus;

public class InternalServerErrorException extends ApplicationHttpException {

    public InternalServerErrorException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
