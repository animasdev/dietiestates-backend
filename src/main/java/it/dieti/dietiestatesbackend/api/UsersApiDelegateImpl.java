package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.UserInfo;
import it.dieti.dietiestatesbackend.api.model.UserInfoAgencyProfile;
import it.dieti.dietiestatesbackend.api.model.UserInfoAgentProfile;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.application.user.UserProfileService;
import it.dieti.dietiestatesbackend.application.user.UserProfileService.AgentProfile;
import it.dieti.dietiestatesbackend.application.user.UserProfileService.AgencyProfile;
import it.dieti.dietiestatesbackend.application.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class UsersApiDelegateImpl implements UsersApiDelegate {
    private final UserService userService;
    private final UserProfileService userProfileService;
    private static final Logger log = LoggerFactory.getLogger(UsersApiDelegateImpl.class);

    public UsersApiDelegateImpl(UserService userService, UserProfileService userProfileService) {
        this.userService = userService;
        this.userProfileService = userProfileService;
    }

    @Override
    public ResponseEntity<UserInfo> usersMeGet() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)){
            log.warn("Accesso a users/me senza token");
            throw UnauthorizedException.bearerTokenMissing();
        }

        var token = jwtAuth.getToken();
        var userId = UUID.fromString(token.getSubject());
        var user = resolveUser(userId);

        var body = new UserInfo();
        body.setDisplayName(user.displayName());
        body.setEmail(user.email());
        body.setRole(resolveRoleCode(user.roleId(), userId));

        userProfileService.findAgencyProfile(userId)
                .ifPresent(profile -> body.setAgencyProfile(toApi(profile)));

        userProfileService.findAgentProfile(userId)
                .ifPresent(profile -> body.setAgentProfile(toApi(profile)));

        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    private it.dieti.dietiestatesbackend.domain.user.User resolveUser(UUID userId) {
        try {
            return userService.findUserById(userId);
        } catch (NoSuchElementException ex) {
            log.warn("Utente {} non trovato durante users/me", userId);
            throw UnauthorizedException.userNotFound();
        }
    }

    private String resolveRoleCode(UUID roleId, UUID userId) {
        try {
            return userService.getRoleCode(roleId);
        } catch (NoSuchElementException ex) {
            log.warn("Ruolo {} non trovato per utente {}", roleId, userId);
            throw UnauthorizedException.userNotFound();
        }
    }

    private UserInfoAgencyProfile toApi(AgencyProfile profile) {
        UserInfoAgencyProfile api = new UserInfoAgencyProfile();
        api.setName(profile.name());
        api.setDescription(profile.description());
        api.setLogoMediaId(profile.logoMediaId());
        return api;
    }

    private UserInfoAgentProfile toApi(AgentProfile profile) {
        UserInfoAgentProfile api = new UserInfoAgentProfile();
        api.setAgencyId(profile.agencyId());
        api.setReaNumber(profile.reaNumber());
        api.setProfilePhotoMediaId(profile.profilePhotoMediaId());
        return api;
    }
}
