package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.*;
import it.dieti.dietiestatesbackend.application.auth.AuthService;
import it.dieti.dietiestatesbackend.application.auth.PasswordResetService;
import it.dieti.dietiestatesbackend.application.auth.RefreshTokenService;
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
import java.util.UUID;

@Service
public class AuthApiDelegateImpl implements AuthApiDelegate {
    private final AuthService authService;
    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenService refreshTokenService;
    private static final Logger log = LoggerFactory.getLogger(AuthApiDelegateImpl.class);

    public AuthApiDelegateImpl(AuthService authService, UserService userService, PasswordResetService passwordResetService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.userService = userService;
        this.passwordResetService = passwordResetService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public ResponseEntity<AuthLoginPost200Response> authLoginPost(AuthLoginPostRequest authLoginPostRequest) {
        try {
            var result = authService.login(authLoginPostRequest.getEmail(), authLoginPostRequest.getPassword());
            AuthLoginPost200Response body = new AuthLoginPost200Response();
            body.setAccessToken(result.accessToken());
            body.setFirstAccess(result.firstAccess());
            if (result.firstAccess()) {
                body.setInvitedBy(result.invitedByUserId());
            } else {
                body.setInvitedBy(null);
            }
            var issue = refreshTokenService.issue(result.userId());
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.SET_COOKIE, issue.cookie().toString())
                    .body(body);
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
    public ResponseEntity<AuthRefreshPost200Response> authRefreshPost() {
        try {
            var request = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (!(request instanceof org.springframework.web.context.request.ServletRequestAttributes attrs)) {
                throw UnauthorizedException.bearerTokenMissing();
            }
            var httpReq = attrs.getRequest();
            String cookieValue = null;
            if (httpReq.getCookies() != null) {
                for (var c : httpReq.getCookies()) {
                    if ("refreshToken".equals(c.getName())) {
                        cookieValue = c.getValue();
                        break;
                    }
                }
            }
            var refreshed = refreshTokenService.refresh(cookieValue);
            var body = new it.dieti.dietiestatesbackend.api.model.AuthRefreshPost200Response();
            body.setAccessToken(refreshed.accessToken());
            // refreshToken field kept for backward compat in schema but not used: do not set
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.SET_COOKIE, refreshed.rotatedCookie().toString())
                    .body(body);
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Errore inatteso durante refresh", ex);
            throw new InternalServerErrorException("Errore durante il refresh del token");
        }
    }

    @Override
    public ResponseEntity<Void> authSignUpRequestPost(AuthSignUpRequestPostRequest request) {
        String requestedRoleCode = resolveRequestedRoleCode(request);
        UUID invitedBy = resolveInvitedBy(requestedRoleCode);
        authService.requestSignUp(request.getEmail(), request.getDisplayName(), requestedRoleCode, invitedBy);
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

    @Override
    public ResponseEntity<Void> authLogoutPost(){
        try {
            var request = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            String raw = null;
            if (request instanceof org.springframework.web.context.request.ServletRequestAttributes attrs && attrs.getRequest().getCookies() != null) {
                for (var c : attrs.getRequest().getCookies()) {
                    if ("refreshToken".equals(c.getName())) { raw = c.getValue(); break; }
                }
            }
            // Revoke this refresh token server-side and expire cookie on client
            refreshTokenService.revokeByRawToken(raw);
            var expired = refreshTokenService.expireCookie();
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.SET_COOKIE, expired.toString())
                    .build();
        } catch (Exception ex) {
            log.warn("Errore durante logout", ex);
            var expired = refreshTokenService.expireCookie();
            return ResponseEntity.ok().header(org.springframework.http.HttpHeaders.SET_COOKIE, expired.toString()).build();
        }
    }

    @Override
    public ResponseEntity<Void> authLogoutAllPost() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Logout-all senza token");
            throw UnauthorizedException.bearerTokenMissing();
        }
        try {
            var userId = java.util.UUID.fromString(jwtAuth.getToken().getSubject());
            refreshTokenService.revokeAllForUser(userId);
        } catch (Exception e) {
            log.warn("Errore durante logout-all", e);
        }
        var expired = refreshTokenService.expireCookie();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, expired.toString())
                .build();
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

    private UUID resolveInvitedBy(String requestedRoleCode) {
        if (requestedRoleCode == null || requestedRoleCode.isBlank()) {
            return null;
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            try {
                return UUID.fromString(jwtAuth.getToken().getSubject());
            } catch (IllegalArgumentException ex) {
                log.warn("Impossibile estrarre l'utente invitante dal token JWT", ex);
            }
        }
        return null;
    }
}
