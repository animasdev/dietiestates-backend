package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.AuthSignUpRequestPostRequest;
import it.dieti.dietiestatesbackend.application.auth.AuthService;
import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.application.user.UserService;
import it.dieti.dietiestatesbackend.domain.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthApiDelegateImplTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthApiDelegateImpl delegate;

    private SecurityContext originalContext;
    private UUID authenticatedUserId;

    @BeforeEach
    void setUp() {
        originalContext = SecurityContextHolder.getContext();
    }

    @AfterEach
    void tearDown() {
        // Restore original context to avoid cross-test leakage
        SecurityContextHolder.clearContext();
        SecurityContextHolder.setContext(originalContext);
    }

    private void mockAuthenticatedCallerWithRoleCode(String roleCode) {
        authenticatedUserId = UUID.randomUUID();
        var callerRoleId = UUID.randomUUID();
        // Mock resolution of caller role via UserService
        when(userService.findUserById(authenticatedUserId))
                .thenReturn(new User(authenticatedUserId, "Caller", "caller@example.com", false, callerRoleId, null, null, null, null, null));
        when(userService.getRoleCode(callerRoleId)).thenReturn(roleCode);

        // Build a minimal Jwt with subject = userId
        Jwt jwt = Jwt.withTokenValue("token")
                .subject(authenticatedUserId.toString())
                .header("alg", "none")
                .claim("sub", authenticatedUserId.toString())
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void superadmin_can_request_signup_for_admin() {
        mockAuthenticatedCallerWithRoleCode("SUPERADMIN");

        AuthSignUpRequestPostRequest req = new AuthSignUpRequestPostRequest();
        req.setEmail("new-admin@example.com");
        req.setDisplayName("New Admin");
        req.setRoleCode("ADMIN");

        delegate.authSignUpRequestPost(req);

        ArgumentCaptor<String> roleArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> inviterArg = ArgumentCaptor.forClass(UUID.class);
        verify(authService, times(1))
                .requestSignUp(eq("new-admin@example.com"), eq("New Admin"), roleArg.capture(), inviterArg.capture());

        assertThat(roleArg.getValue()).isEqualTo("ADMIN");
        assertThat(inviterArg.getValue()).isEqualTo(authenticatedUserId);
    }

    @Test
    void superadmin_can_request_signup_for_agency() {
        mockAuthenticatedCallerWithRoleCode("SUPERADMIN");

        AuthSignUpRequestPostRequest req = new AuthSignUpRequestPostRequest();
        req.setEmail("new-agency@example.com");
        req.setDisplayName("New Agency");
        req.setRoleCode("AGENCY");

        delegate.authSignUpRequestPost(req);

        ArgumentCaptor<String> roleArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> inviterArg = ArgumentCaptor.forClass(UUID.class);
        verify(authService, times(1))
                .requestSignUp(eq("new-agency@example.com"), eq("New Agency"), roleArg.capture(), inviterArg.capture());

        assertThat(roleArg.getValue()).isEqualTo("AGENCY");
        assertThat(inviterArg.getValue()).isEqualTo(authenticatedUserId);
    }

    @Test
    void admin_can_request_signup_for_agency() {
        mockAuthenticatedCallerWithRoleCode("ADMIN");

        AuthSignUpRequestPostRequest req = new AuthSignUpRequestPostRequest();
        req.setEmail("agency@example.com");
        req.setDisplayName("Agency");
        req.setRoleCode("AGENCY");

        delegate.authSignUpRequestPost(req);

        ArgumentCaptor<String> roleArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> inviterArg = ArgumentCaptor.forClass(UUID.class);
        verify(authService, times(1))
                .requestSignUp(eq("agency@example.com"), eq("Agency"), roleArg.capture(), inviterArg.capture());

        assertThat(roleArg.getValue()).isEqualTo("AGENCY");
        assertThat(inviterArg.getValue()).isEqualTo(authenticatedUserId);
    }

    @Test
    void agency_can_request_signup_for_agent() {
        mockAuthenticatedCallerWithRoleCode("AGENCY");

        AuthSignUpRequestPostRequest req = new AuthSignUpRequestPostRequest();
        req.setEmail("agent@example.com");
        req.setDisplayName("Agent");
        req.setRoleCode("AGENT");

        delegate.authSignUpRequestPost(req);

        ArgumentCaptor<String> roleArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> inviterArg = ArgumentCaptor.forClass(UUID.class);
        verify(authService, times(1))
                .requestSignUp(eq("agent@example.com"), eq("Agent"), roleArg.capture(), inviterArg.capture());

        // Desired behavior: agency can create agent. If this fails, update RolesPolicy accordingly.
        assertThat(roleArg.getValue()).isEqualTo("AGENT");
        assertThat(inviterArg.getValue()).isEqualTo(authenticatedUserId);
    }

    @Test
    void user_request_user_role_defaults_to_user() {
        mockAuthenticatedCallerWithRoleCode("USER");

        AuthSignUpRequestPostRequest req = new AuthSignUpRequestPostRequest();
        req.setEmail("user@example.com");
        req.setDisplayName("User");
        req.setRoleCode("USER");

        assertThatThrownBy(() -> delegate.authSignUpRequestPost(req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Permesso negato");

        verify(authService, never()).requestSignUp(anyString(), anyString(), anyString(), any(UUID.class));
    }

    @Test
    void user_cannot_request_non_user_roles() {
        mockAuthenticatedCallerWithRoleCode("USER");

        AuthSignUpRequestPostRequest req1 = new AuthSignUpRequestPostRequest();
        req1.setEmail("a1@example.com");
        req1.setDisplayName("A1");
        req1.setRoleCode("ADMIN");

        AuthSignUpRequestPostRequest req2 = new AuthSignUpRequestPostRequest();
        req2.setEmail("a2@example.com");
        req2.setDisplayName("A2");
        req2.setRoleCode("AGENCY");

        AuthSignUpRequestPostRequest req3 = new AuthSignUpRequestPostRequest();
        req3.setEmail("a3@example.com");
        req3.setDisplayName("A3");
        req3.setRoleCode("AGENT");

        assertThatThrownBy(() -> delegate.authSignUpRequestPost(req1))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Permesso negato");

        assertThatThrownBy(() -> delegate.authSignUpRequestPost(req2))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Permesso negato");

        assertThatThrownBy(() -> delegate.authSignUpRequestPost(req3))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Permesso negato");

        verify(authService, never()).requestSignUp(anyString(), anyString(), anyString(), any(UUID.class));
    }

    @Test
    void anonymous_request_has_null_invitedBy_and_role() {
        AuthSignUpRequestPostRequest req = new AuthSignUpRequestPostRequest();
        req.setEmail("someone@example.com");
        req.setDisplayName("Someone");
        // roleCode left null by design

        delegate.authSignUpRequestPost(req);

        verify(authService).requestSignUp(eq("someone@example.com"), eq("Someone"), isNull(), isNull());
    }
}
