package it.dieti.dietiestatesbackend.application.auth;

import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.domain.auth.RefreshToken;
import it.dieti.dietiestatesbackend.domain.auth.RefreshTokenRepository;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final JwtEncoder jwtEncoder;
    private final it.dieti.dietiestatesbackend.config.JwtConfig jwtConfig;

    private static final String COOKIE_NAME = "refreshToken";
    private static final Duration REFRESH_TTL = Duration.ofDays(30);

    public RefreshTokenService(RefreshTokenRepository repository,
                               JwtEncoder jwtEncoder,
                               it.dieti.dietiestatesbackend.config.JwtConfig jwtConfig) {
        this.repository = repository;
        this.jwtEncoder = jwtEncoder;
        this.jwtConfig = jwtConfig;
    }

    public record IssueResult(String cookieValue, ResponseCookie cookie) {}
    public record RefreshResult(String accessToken, ResponseCookie rotatedCookie) {}

    @Transactional
    public IssueResult issue(UUID userId) {
        String raw = generateOpaqueToken();
        String hash = sha256(raw);
        var now = OffsetDateTime.now();
        var toInsert = new RefreshToken(null, userId, hash, now.plus(REFRESH_TTL), null, now, now);
        repository.insert(toInsert);
        return new IssueResult(raw, buildCookie(raw, REFRESH_TTL));
    }

    @Transactional
    public RefreshResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw UnauthorizedException.refreshTokenMissing();
        }
        var now = OffsetDateTime.now();
        String hash = sha256(rawToken);
        var active = repository.findActiveByHash(hash, now).orElseThrow(UnauthorizedException::bearerTokenMissing);

        // Rotate: revoke current and create a new one
        var revoked = new RefreshToken(
                active.id(), active.userId(), active.tokenHash(), active.expiresAt(), now, active.createdAt(), now
        );
        repository.update(revoked);

        String newRaw = generateOpaqueToken();
        String newHash = sha256(newRaw);
        var inserted = new RefreshToken(null, active.userId(), newHash, now.plus(REFRESH_TTL), null, now, now);
        repository.insert(inserted);
        ResponseCookie rotated = buildCookie(newRaw, REFRESH_TTL);

        // New short-lived access token
        String accessToken = generateAccessToken(active.userId());
        return new RefreshResult(accessToken, rotated);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repository.revokeAllForUser(userId, OffsetDateTime.now());
    }

    @Transactional
    public void revokeByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        repository.revokeByHash(sha256(rawToken), OffsetDateTime.now());
    }

    public ResponseCookie expireCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false) // set true behind HTTPS in prod
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private ResponseCookie buildCookie(String raw, Duration ttl) {
        return ResponseCookie.from(COOKIE_NAME, raw)
                .httpOnly(true)
                .secure(false) // set true behind HTTPS in prod
                .sameSite("Lax")
                .path("/")
                .maxAge(ttl)
                .build();
    }

    private String generateAccessToken(UUID userId) {
        Instant now = Instant.now();
        long seconds = clamp(jwtConfig.getExpiresSeconds(), 600, 3600); // clamp 10–60 min
        Instant exp = now.plusSeconds(seconds);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtConfig.getIssuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(userId.toString())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static long clamp(long value, long min, long max) {
        if (min > max) {
            long tmp = min; min = max; max = tmp;
        }
        return Math.max(min, Math.min(value, max));
    }

    private static String generateOpaqueToken() {
        byte[] buf = new byte[32];
        new SecureRandom().nextBytes(buf);
        return HexFormat.of().withUpperCase().formatHex(buf);
    }

    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
