package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.AuthLoginPost200Response;
import it.dieti.dietiestatesbackend.api.model.AuthLoginPostRequest;
import it.dieti.dietiestatesbackend.api.model.AuthSignUpConfirmPostRequest;
import it.dieti.dietiestatesbackend.api.model.AuthSignUpRequestPostRequest;
import it.dieti.dietiestatesbackend.application.auth.AuthService;
import it.dieti.dietiestatesbackend.application.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthApiDelegateImpl implements AuthApiDelegate {
    private final AuthService authService;
    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(AuthApiDelegateImpl.class);

    public AuthApiDelegateImpl(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
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
            String requestedRoleCode = null;
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                var roleOfCaller = resolveCurrentUserRoleCode(jwtAuth);
                if (roleOfCaller != null && "SUPERADMIN".equalsIgnoreCase(roleOfCaller)) {
                    requestedRoleCode = request.getRoleCode();
                }
            }
            authService.requestSignUp(request.getEmail(), request.getDisplayName(), requestedRoleCode);
            return ResponseEntity.accepted().build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            log.warn("authSignUpRequestPost failed, returning 202 to avoid enumeration", ex);
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
            log.warn("authSignUpConfirmPost failed", ex);
            return ResponseEntity.badRequest().build();
        }
    }

    private String resolveCurrentUserRoleCode(JwtAuthenticationToken jwtAuth) {
        try {
            var userId = java.util.UUID.fromString(jwtAuth.getToken().getSubject());
            return userService.getRoleCode(userService.findUserById(userId).roleId());
        } catch (Exception e) {
            log.debug("Failed to resolve caller role from JWT", e);
            return null;
        }
    }
}
