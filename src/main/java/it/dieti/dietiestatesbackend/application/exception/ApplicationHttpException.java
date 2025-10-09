package it.dieti.dietiestatesbackend.application.exception;

import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;

public abstract class ApplicationHttpException extends RuntimeException {

    private final HttpStatus status;
    private final List<FieldErrorDetail> fieldErrors;

    protected ApplicationHttpException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    protected ApplicationHttpException(HttpStatus status, String message, List<FieldErrorDetail> fieldErrors) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public HttpStatus status() {
        return status;
    }

    public List<FieldErrorDetail> fieldErrors() {
        return Collections.unmodifiableList(fieldErrors);
    }

    public record FieldErrorDetail(String field, String message) {}
}
