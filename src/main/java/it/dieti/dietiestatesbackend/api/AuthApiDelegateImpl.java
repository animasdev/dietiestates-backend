package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.AuthLoginPost200Response;
import it.dieti.dietiestatesbackend.api.model.AuthLoginPostRequest;
import it.dieti.dietiestatesbackend.api.model.AuthSignUpConfirmPostRequest;
import it.dieti.dietiestatesbackend.api.model.AuthSignUpRequestPostRequest;
import it.dieti.dietiestatesbackend.application.auth.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class AuthApiDelegateImpl implements AuthApiDelegate {
    private final AuthService authService;

    public AuthApiDelegateImpl(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<AuthLoginPost200Response> authLoginPost(AuthLoginPostRequest authLoginPostRequest) {
        try {
            var result = authService.login(authLoginPostRequest.getEmail(), authLoginPostRequest.getPassword());
            AuthLoginPost200Response body = new AuthLoginPost200Response();
            body.setAccessToken(result.accessToken());

            body.getClass().getMethod("setFirstAccess", Boolean.class).invoke(body, result.firstAccess());

            return ResponseEntity.ok(body);
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<Void> authSignUpRequestPost(AuthSignUpRequestPostRequest request) {
        try {
            authService.requestSignUp(request.getEmail(), request.getDisplayName());
            return ResponseEntity.accepted().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
    }

    @Override
    public ResponseEntity<Void> authSignUpConfirmPost(AuthSignUpConfirmPostRequest request) {
        try {
            boolean ok = authService.confirmSignUp(request.getToken(), request.getPassword());
            if (ok) return ResponseEntity.ok().build();
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
