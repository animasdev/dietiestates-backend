package it.dieti.dietiestatesbackend.domain.auth;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    Optional<PasswordResetToken> findActiveByUser(UUID userId, OffsetDateTime now);
    Optional<PasswordResetToken> findActiveByToken(String token, OffsetDateTime now);
    PasswordResetToken insert(PasswordResetToken token);
    PasswordResetToken update(PasswordResetToken token);
    void delete(UUID id);
}

