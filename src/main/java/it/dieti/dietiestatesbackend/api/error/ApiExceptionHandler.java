package it.dieti.dietiestatesbackend.api.error;

import it.dieti.dietiestatesbackend.application.exception.ApplicationHttpException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.metadata.ConstraintDescriptor;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String GENERIC_VALIDATION_MESSAGE = "Richiesta non valida: alcuni campi sono mancanti o errati.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return buildValidationResponse(ex.getBindingResult().getFieldErrors(), GENERIC_VALIDATION_MESSAGE);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(BindException ex) {
        return buildValidationResponse(ex.getBindingResult().getFieldErrors(), GENERIC_VALIDATION_MESSAGE);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations().stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());
        var response = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, GENERIC_VALIDATION_MESSAGE, errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        var response = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, "Payload non leggibile o mancante.", null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ApplicationHttpException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationHttpException(ApplicationHttpException ex) {
        var errors = ex.fieldErrors().stream()
                .map(detail -> new ApiFieldError(detail.field(), detail.message()))
                .collect(Collectors.toList());
        var response = ApiErrorResponse.of(ex.status(), ex.getMessage(), errors);
        return ResponseEntity.status(ex.status()).body(response);
    }

    private ResponseEntity<ApiErrorResponse> buildValidationResponse(List<FieldError> fieldErrors, String message) {
        var errors = fieldErrors.stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());
        var response = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, message, errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private ApiFieldError toFieldError(FieldError fieldError) {
        var message = resolveMessage(fieldError.getCode(), fieldError.getDefaultMessage(), fieldError.getField());
        return new ApiFieldError(fieldError.getField(), message);
    }

    private ApiFieldError toFieldError(ConstraintViolation<?> violation) {
        var field = extractLeafProperty(violation.getPropertyPath().toString());
        String code = null;
        ConstraintDescriptor<?> descriptor = violation.getConstraintDescriptor();
        if (descriptor != null && descriptor.getAnnotation() != null) {
            code = descriptor.getAnnotation().annotationType().getSimpleName();
        }
        var message = resolveMessage(code, violation.getMessage(), field);
        return new ApiFieldError(field, message);
    }

    private String extractLeafProperty(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        var normalized = path;
        var lastDot = normalized.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < normalized.length() - 1) {
            return normalized.substring(lastDot + 1);
        }
        return normalized;
    }

    private String resolveMessage(@Nullable String code, @Nullable String defaultMessage, String field) {
        if (code != null) {
            var normalizedCode = code.toLowerCase(Locale.ROOT);
            if (normalizedCode.contains("notnull") || normalizedCode.contains("notblank") || normalizedCode.contains("notempty")) {
                return "Il campo '" + field + "' è obbligatorio.";
            }
            if (normalizedCode.contains("size")) {
                return defaultMessage != null && !defaultMessage.isBlank()
                        ? defaultMessage
                        : "Il campo '" + field + "' ha una lunghezza non valida.";
            }
            if (normalizedCode.contains("min") || normalizedCode.contains("max")) {
                return defaultMessage != null && !defaultMessage.isBlank()
                        ? defaultMessage
                        : "Il campo '" + field + "' non rispetta i vincoli numerici.";
            }
        }
        if (defaultMessage != null && !defaultMessage.isBlank()) {
            return defaultMessage;
        }
        return "Valore non valido per il campo '" + field + "'.";
    }
}
