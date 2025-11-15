package it.dieti.dietiestatesbackend.domain.auth;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken insert(RefreshToken token);
    void update(RefreshToken token);
    Optional<RefreshToken> findByHash(String tokenHash);
    Optional<RefreshToken> findActiveByHash(String tokenHash, OffsetDateTime now);
    void revokeAllForUser(UUID userId, OffsetDateTime when);
    void revokeByHash(String tokenHash, OffsetDateTime when);
}
