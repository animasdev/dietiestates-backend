package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.application.user.UserProfileService;
import it.dieti.dietiestatesbackend.application.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UsersApiDelegateImplTest {

    @Mock
    private UserService userService;

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private UsersApiDelegateImpl delegate;

    private SecurityContext originalContext;

    @BeforeEach
    void setUp() {
        originalContext = SecurityContextHolder.getContext();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.setContext(originalContext);
    }

    @Test
    void usersMeGet_withoutJwtAuthentication_throwsUnauthorizedException() {
        SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(emptyContext);

        assertThatThrownBy(() -> delegate.usersMeGet())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Autenticazione richiesta: fornire un bearer token valido.");
    }
}

