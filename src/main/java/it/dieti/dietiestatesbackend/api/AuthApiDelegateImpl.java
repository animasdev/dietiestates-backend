package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.*;
import it.dieti.dietiestatesbackend.application.auth.AuthService;
import it.dieti.dietiestatesbackend.application.auth.PasswordResetService;
import it.dieti.dietiestatesbackend.application.user.UserService;
import it.dieti.dietiestatesbackend.domain.user.role.RolesPolicy;
import it.dieti.dietiestatesbackend.domain.user.role.RolesEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthApiDelegateImpl implements AuthApiDelegate {
    private final AuthService authService;
    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private static final Logger log = LoggerFactory.getLogger(AuthApiDelegateImpl.class);

    public AuthApiDelegateImpl(AuthService authService, UserService userService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.userService = userService;
        this.passwordResetService = passwordResetService;
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
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                var roleOfCaller = resolveCurrentUserRoleCode(jwtAuth);
                if (RolesPolicy.isAllowedCreate(roleOfCaller,RolesEnum.valueOf(request.getRoleCode()))) {
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

    @Override
    public ResponseEntity<Void> authPasswordResetRequestPost(AuthPasswordResetRequestPostRequest authPasswordResetRequestPostRequest){
            String emailFromBody = authPasswordResetRequestPostRequest.getEmail();
            passwordResetService.requestPasswordResetByEmail(emailFromBody);
            return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> authPasswordResetConfirmPost(AuthPasswordResetConfirmPostRequest authPasswordResetConfirmPostRequest) {
        try {
            String token = authPasswordResetConfirmPostRequest.getToken();
            String newPassword = authPasswordResetConfirmPostRequest.getNewPassword();
            boolean ok = passwordResetService.confirmPasswordReset(token, newPassword);
            if (ok) return ResponseEntity.ok().build();
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.warn("authPasswordResetConfirmPost failed", ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<Void> authPasswordPut(AuthPasswordPutRequest authPasswordPutRequest) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String oldPassword = authPasswordPutRequest.getOldPassword();
            String newPassword = authPasswordPutRequest.getNewPassword();
            var userId = java.util.UUID.fromString(jwtAuth.getToken().getSubject());
            boolean ok = passwordResetService.changePasswordForUser(userId, oldPassword, newPassword);
            if (ok) return ResponseEntity.ok().build();
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.warn("authPasswordPut failed", ex);
            return ResponseEntity.badRequest().build();
        }
    }

    private RolesEnum resolveCurrentUserRoleCode(JwtAuthenticationToken jwtAuth) {
        try {
            var userId = java.util.UUID.fromString(jwtAuth.getToken().getSubject());
            var roleCode =  userService.getRoleCode(userService.findUserById(userId).roleId());
            return RolesEnum.valueOf(roleCode);
        } catch (Exception e) {
            log.debug("Failed to resolve caller role from JWT", e);
            return null;
        }
    }
}
