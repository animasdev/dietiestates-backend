package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.UserInfo;
import it.dieti.dietiestatesbackend.api.model.UserInfoAgencyProfile;
import it.dieti.dietiestatesbackend.api.model.UserInfoAgentProfile;
import it.dieti.dietiestatesbackend.application.user.UserProfileService;
import it.dieti.dietiestatesbackend.application.user.UserProfileService.AgentProfile;
import it.dieti.dietiestatesbackend.application.user.UserProfileService.AgencyProfile;
import it.dieti.dietiestatesbackend.application.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UsersApiDelegateImpl implements UsersApiDelegate {
    private final UserService userService;
    private final UserProfileService userProfileService;

    public UsersApiDelegateImpl(UserService userService, UserProfileService userProfileService) {
        this.userService = userService;
        this.userProfileService = userProfileService;
    }

    @Override
    public ResponseEntity<UserInfo> usersMeGet() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        var token = jwtAuth.getToken();
        var userId = UUID.fromString(token.getSubject());
        var user = userService.findUserById(userId);

        var body = new UserInfo();
        body.setDisplayName(user.displayName());
        body.setEmail(user.email());
        body.setRole(userService.getRoleCode(user.roleId()));

        userProfileService.findAgencyProfile(userId)
                .ifPresent(profile -> body.setAgencyProfile(toApi(profile)));

        userProfileService.findAgentProfile(userId)
                .ifPresent(profile -> body.setAgentProfile(toApi(profile)));

        return new ResponseEntity<>(body, HttpStatus.OK);
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
