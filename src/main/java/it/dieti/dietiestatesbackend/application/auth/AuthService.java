package it.dieti.dietiestatesbackend.application.auth;

import it.dieti.dietiestatesbackend.application.notification.NotificationService;
import it.dieti.dietiestatesbackend.domain.auth.SignUpToken;
import it.dieti.dietiestatesbackend.domain.auth.SignUpTokenRepository;
import it.dieti.dietiestatesbackend.domain.user.User;
import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RoleRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SignUpTokenRepository signUpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final it.dieti.dietiestatesbackend.config.JwtConfig jwtConfig;
    private final it.dieti.dietiestatesbackend.config.SecurityPasswordProperties passwordProps;
    private final NotificationService notificationService = new NotificationService();

    private static final long SIGNUP_TOKEN_TTL_MINUTES = 30; // can become configurable later

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       SignUpTokenRepository signUpTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       it.dieti.dietiestatesbackend.config.JwtConfig jwtConfig,
                       it.dieti.dietiestatesbackend.config.SecurityPasswordProperties passwordProps) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.signUpTokenRepository = signUpTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtConfig = jwtConfig;
        this.passwordProps = passwordProps;
    }

    public record AuthLoginResult(String accessToken, boolean firstAccess) {}

    public AuthLoginResult login(String email, String password) {
        var normalized = email == null ? "" : email.trim();
        var user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.passwordHash() == null || user.passwordHash().isBlank()) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtConfig.getExpiresSeconds());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtConfig.getIssuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(user.id().toString())
                .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthLoginResult(token, user.firstAccess());
    }

    @Transactional
    public void requestSignUp(String email, String displayName, String requestedRoleCodeOrNull) {
        var normalized = email == null ? "" : email.trim();
        var name = displayName == null ? "" : displayName.trim();
        if (normalized.isBlank() || name.isBlank()) {
            return; // ignore invalid; controller returns 202 regardless
        }
        // If already registered, do nothing but still return 202 at API level
        if (userRepository.existsByEmail(normalized)) {
            return;
        }
        // Resolve role by code (API stays with roleCode), but persist roleId in token
        var effectiveRole = roleRepository.findByCode(
                (requestedRoleCodeOrNull == null || requestedRoleCodeOrNull.isBlank()) ? "USER" : requestedRoleCodeOrNull.trim()
        ).orElseThrow(() -> new IllegalArgumentException("Role does not exist"));
        var now = OffsetDateTime.now();
        var existing = signUpTokenRepository.findActiveByEmail(normalized, now);
        if (existing.isPresent()) {
            // Refresh expiration, keep token value unchanged
            var t = existing.get();
            var refreshed = new SignUpToken(
                    t.id(), t.email(), t.displayName(), t.token(), t.roleId(),
                    now.plusMinutes(SIGNUP_TOKEN_TTL_MINUTES), t.consumedAt(), t.createdAt(), now
            );
            signUpTokenRepository.update(refreshed);
            notificationService.sendSignUpConfirmation(normalized, t.token());
            return;
        }

        String token = generateToken();
        var toInsert = new SignUpToken(
                null, normalized, name, token, effectiveRole.id(),
                now.plusMinutes(SIGNUP_TOKEN_TTL_MINUTES), null, now, now
        );
        signUpTokenRepository.insert(toInsert);
        notificationService.sendSignUpConfirmation(normalized, token);
    }

    @Transactional
    public boolean confirmSignUp(String token, String rawPassword) {
        if (token == null || token.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return false;
        }
        var now = OffsetDateTime.now();
        var opt = signUpTokenRepository.findActiveByToken(token, now);
        if (opt.isEmpty()) return false;
        var t = opt.get();
        // If somehow user already exists, consider token consumed and return true
        if (!userRepository.existsByEmail(t.email())) {
            var user = new User(
                    null,
                    t.displayName(),
                    t.email(),
                    false,
                    t.roleId(),
                    passwordEncoder.encode(rawPassword),
                    passwordProps.getPasswordAlgo(),
                    null,
                    null
            );
            userRepository.insert(user);
        }
        var consumed = new SignUpToken(
                t.id(), t.email(), t.displayName(), t.token(), t.roleId(), t.expiresAt(), now, t.createdAt(), now
        );
        signUpTokenRepository.update(consumed);
        return true;
    }

    private static String generateToken() {
        byte[] buf = new byte[32];
        new SecureRandom().nextBytes(buf);
        return HexFormat.of().withUpperCase().formatHex(buf);
    }
}
