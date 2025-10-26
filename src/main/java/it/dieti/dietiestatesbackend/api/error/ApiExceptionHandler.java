package it.dieti.dietiestatesbackend.api.error;

import it.dieti.dietiestatesbackend.application.exception.ApplicationHttpException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.metadata.ConstraintDescriptor;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String GENERIC_VALIDATION_MESSAGE = "Richiesta non valida: alcuni campi sono mancanti o errati.";
    private static final String GENERIC_INTERNAL_ERROR_MESSAGE = "Si è verificato un errore interno. Riprova più tardi.";
    private static final String THE_FIELD = "Il campo";
    private static final String IS_MANDATORY = "è obbligatorio";

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validazione request fallita: {}", ex.getMessage());
        return buildValidationResponse(ex.getBindingResult().getFieldErrors(), GENERIC_VALIDATION_MESSAGE);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(BindException ex) {
        log.warn("Binding request fallito: {}", ex.getMessage());
        return buildValidationResponse(ex.getBindingResult().getFieldErrors(), GENERIC_VALIDATION_MESSAGE);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations().stream()
                .map(this::toFieldError)
                .toList();
        log.warn("Constraint violation: {}", ex.getMessage());
        var response = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, GENERIC_VALIDATION_MESSAGE, errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Payload non leggibile o mancante", ex);
        var response = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, "Payload non leggibile, mancante o non conforme alle specifiche.", null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ApplicationHttpException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationHttpException(ApplicationHttpException ex) {
        var errors = ex.fieldErrors().stream()
                .map(detail -> new ApiFieldError(detail.field(), detail.message()))
                .toList();
        var response = ApiErrorResponse.of(ex.status(), ex.getMessage(), errors);
        logException(ex.status(), ex.getMessage(), ex);
        return ResponseEntity.status(ex.status()).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        var fieldName = ex.getName();
        var message = "Il parametro '" + fieldName + "' ha un formato non valido.";
        log.warn("Parametro {} con tipo non valido: {}", fieldName, ex.getMessage());
        var response = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, message, List.of(new ApiFieldError(fieldName, message)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        var field = ex.getParameterName();
        var message = "Il parametro '" + field + "'" + IS_MANDATORY;
        log.warn("Parametro mancante: {}", field);
        var response = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, message, List.of(new ApiFieldError(field, message)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingServletRequestPart(MissingServletRequestPartException ex) {
        var field = ex.getRequestPartName();
        var message = "La parte '" + field + "'" + IS_MANDATORY;
        log.warn("Parte mancante: {}", field);
        var response = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, message, List.of(new ApiFieldError(field, message)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        var message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = GENERIC_VALIDATION_MESSAGE;
        }
        log.warn("Richiesta non valida: {}", message);
        var response = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, message, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        var message = "Permesso negato.";
        log.warn("Accesso negato: {}", ex.getMessage());
        var response = ApiErrorResponse.of(HttpStatus.FORBIDDEN, message, null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Errore non gestito", ex);
        var response = ApiErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_INTERNAL_ERROR_MESSAGE, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ResponseEntity<ApiErrorResponse> buildValidationResponse(List<FieldError> fieldErrors, String message) {
        var errors = fieldErrors.stream()
                .map(this::toFieldError)
                .toList();
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
            String message = messageFromCode(code, defaultMessage, field);
            if (message != null) return message;
        }
        if (defaultMessage != null && !defaultMessage.isBlank()) {
            return defaultMessage;
        }
        return "Valore non valido per il campo '" + field + "'.";
    }

    private static String messageFromCode(String code, String defaultMessage, String field) {
        var normalizedCode = code.toLowerCase(Locale.ROOT);
        if (normalizedCode.contains("notnull") || normalizedCode.contains("notblank") || normalizedCode.contains("notempty")) {
            return THE_FIELD + "'" + field + "'" + IS_MANDATORY;
        }
        if (normalizedCode.contains("size")) {
            return defaultMessage != null && !defaultMessage.isBlank()
                    ? defaultMessage
                    : THE_FIELD + "'" + field + "' ha una lunghezza non valida.";
        }
        if (normalizedCode.contains("min") || normalizedCode.contains("max")) {
            return defaultMessage != null && !defaultMessage.isBlank()
                    ? defaultMessage
                    : THE_FIELD + "'" + field + "' non rispetta i vincoli numerici.";
        }
        return null;
    }

    private void logException(HttpStatus status, String message, Exception ex) {
        if (status.is5xxServerError()) {
            log.error("Errore API {}: {}", status.value(), message, ex);
        } else {
            log.warn("Errore API {}: {}", status.value(), message);
        }
    }
}
