package it.dieti.dietiestatesbackend.application.auth;

import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final it.dieti.dietiestatesbackend.config.JwtConfig jwtConfig;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       it.dieti.dietiestatesbackend.config.JwtConfig jwtConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtConfig = jwtConfig;
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
}
