package it.dieti.dietiestatesbackend.application.exception;

import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApplicationHttpException {

    private ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

    public static ForbiddenException actionRequiresRole(String requiredRole) {
        return new ForbiddenException("Permesso negato: è richiesto il ruolo " + requiredRole + ".");
    }

    public static ForbiddenException actionRequiresRoles(Collection<String> requiredRoles) {
        var roles = requiredRoles == null || requiredRoles.isEmpty()
                ? "specifici"
                : requiredRoles.stream().distinct().sorted().collect(Collectors.joining(", "));
        return new ForbiddenException("Permesso negato: sono richiesti i ruoli " + roles + ".");
    }

    public static ForbiddenException of(String message) {
        return new ForbiddenException(message);
    }
}
