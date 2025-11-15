package it.dieti.dietiestatesbackend.application.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApplicationHttpException {

    private UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    public static UnauthorizedException bearerTokenMissing() {
        return new UnauthorizedException("Autenticazione richiesta: fornire un bearer token valido.");
    }

    public static UnauthorizedException refreshTokenMissing() {
        return new UnauthorizedException("Refresh token cookie mancante.");
    }

    public static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("Credenziali non valide.");
    }

    public static UnauthorizedException userNotFound() {
        return new UnauthorizedException("Utente non trovato o non più valido.");
    }

    public static UnauthorizedException notTheOwner() {
        return new UnauthorizedException("Utente non è il proprietario dela risorsa");
    }
}
