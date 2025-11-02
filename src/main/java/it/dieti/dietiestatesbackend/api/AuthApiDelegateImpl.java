package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.*;
import it.dieti.dietiestatesbackend.application.auth.AuthService;
import it.dieti.dietiestatesbackend.application.auth.PasswordResetService;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.application.exception.InternalServerErrorException;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.application.user.UserService;
import it.dieti.dietiestatesbackend.domain.user.role.RolesPolicy;
import it.dieti.dietiestatesbackend.domain.user.role.RolesEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import java.util.Arrays;

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
            body.setFirstAccess(result.firstAccess());
            return ResponseEntity.ok(body);
        } catch (BadCredentialsException ex) {
            log.warn("Login fallito per email {}", authLoginPostRequest.getEmail());
            throw UnauthorizedException.invalidCredentials();
        } catch (UnauthorizedException | BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Errore inatteso durante login", ex);
            throw new InternalServerErrorException("Si è verificato un errore interno. Riprova più tardi.");
        }
    }

    @Override
    public ResponseEntity<Void> authSignUpRequestPost(AuthSignUpRequestPostRequest request) {
        String requestedRoleCode = resolveRequestedRoleCode(request);
        authService.requestSignUp(request.getEmail(), request.getDisplayName(), requestedRoleCode);
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<AuthSignUpConfirmPost200Response> authSignUpConfirmPost(AuthSignUpConfirmPostRequest request) {
        if (authService.confirmSignUp(request.getToken(), request.getPassword())) {
            var response = new AuthSignUpConfirmPost200Response().status("confirmed");
            return ResponseEntity.ok(response);
        }
        log.warn("Conferma sign-up fallita per token {}", request.getToken());
        throw BadRequestException.of("Token di conferma non valido o scaduto.");
    }

    @Override
    public ResponseEntity<Void> authPasswordResetRequestPost(AuthPasswordResetRequestPostRequest authPasswordResetRequestPostRequest){
            String emailFromBody = authPasswordResetRequestPostRequest.getEmail();
            passwordResetService.requestPasswordResetByEmail(emailFromBody);
            return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> authPasswordResetConfirmPost(AuthPasswordResetConfirmPostRequest authPasswordResetConfirmPostRequest) {
        String token = authPasswordResetConfirmPostRequest.getToken();
        String newPassword = authPasswordResetConfirmPostRequest.getNewPassword();
        if (passwordResetService.confirmPasswordReset(token, newPassword)) {
            return ResponseEntity.ok().build();
        }
        log.warn("Conferma reset password fallita per token {}", token);
        throw BadRequestException.of("Token di reset non valido o scaduto.");
    }

    @Override
    public ResponseEntity<Void> authPasswordPut(AuthPasswordPutRequest authPasswordPutRequest) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Cambio password senza token");
            throw UnauthorizedException.bearerTokenMissing();
        }
        String oldPassword = authPasswordPutRequest.getOldPassword();
        String newPassword = authPasswordPutRequest.getNewPassword();
        var userId = java.util.UUID.fromString(jwtAuth.getToken().getSubject());
        if (passwordResetService.changePasswordForUser(userId, oldPassword, newPassword)) {
            return ResponseEntity.ok().build();
        }
        log.warn("Cambio password fallito per user {}", userId);
        throw BadRequestException.of("Password non aggiornata: verifica la password attuale e i requisiti della nuova password.");
    }

    private String resolveRequestedRoleCode(AuthSignUpRequestPostRequest request) {
        var roleCode = request.getRoleCode();
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }

        RolesEnum requestedRole;
        try {
            requestedRole = RolesEnum.valueOf(roleCode.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("roleCode {} non valido", roleCode);
            throw BadRequestException.forField("roleCode", "Valore roleCode non valido. Valori ammessi: SUPERADMIN, ADMIN, AGENCY, AGENT.");
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        var allowedRoles = computeAllowedRoles(requestedRole);
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("roleCode {} specificato senza autenticazione", roleCode);
            throw ForbiddenException.actionRequiresRoles(allowedRoles);
        }
        var callerRole = resolveCurrentUserRoleCode(jwtAuth);
        if (callerRole == null || !RolesPolicy.isAllowedCreate(callerRole, requestedRole)) {
            log.warn("Ruolo {} non autorizzato a invitare {}", callerRole, requestedRole);
            throw ForbiddenException.actionRequiresRoles(allowedRoles);
        }
        return requestedRole.name();
    }

    private java.util.List<String> computeAllowedRoles(RolesEnum requestedRole) {
        return Arrays.stream(RolesEnum.values())
                .filter(role -> RolesPolicy.isAllowedCreate(role, requestedRole))
                .map(RolesEnum::name)
                .toList();
    }

    private RolesEnum resolveCurrentUserRoleCode(JwtAuthenticationToken jwtAuth) {
        try {
            var userId = java.util.UUID.fromString(jwtAuth.getToken().getSubject());
            var roleCode = userService.getRoleCode(userService.findUserById(userId).roleId());
            return RolesEnum.valueOf(roleCode);
        } catch (Exception e) {
            log.debug("Failed to resolve caller role from JWT", e);
            return null;
        }
    }
}
