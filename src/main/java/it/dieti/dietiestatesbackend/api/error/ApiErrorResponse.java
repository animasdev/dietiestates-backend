package it.dieti.dietiestatesbackend.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        List<ApiFieldError> errors
) {
    public static ApiErrorResponse of(HttpStatus status, String message, List<ApiFieldError> errors) {
        return new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                errors == null || errors.isEmpty() ? null : List.copyOf(errors)
        );
    }
}
