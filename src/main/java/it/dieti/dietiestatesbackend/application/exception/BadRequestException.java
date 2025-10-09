package it.dieti.dietiestatesbackend.application.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class BadRequestException extends ApplicationHttpException {

    private BadRequestException(String message, List<FieldErrorDetail> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, message, fieldErrors);
    }

    public static BadRequestException of(String message) {
        return new BadRequestException(message, List.of());
    }

    public static BadRequestException forField(String field, String message) {
        return new BadRequestException(message, List.of(new FieldErrorDetail(field, message)));
    }

    public static BadRequestException forFields(String message, List<FieldErrorDetail> fieldErrors) {
        return new BadRequestException(message, fieldErrors);
    }
}
