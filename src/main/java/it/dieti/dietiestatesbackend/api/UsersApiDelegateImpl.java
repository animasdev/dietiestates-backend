package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.UserInfo;
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

    public UsersApiDelegateImpl(UserService userService) {
        this.userService = userService;
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

        return new ResponseEntity<>(body, HttpStatus.OK);
    }
}
