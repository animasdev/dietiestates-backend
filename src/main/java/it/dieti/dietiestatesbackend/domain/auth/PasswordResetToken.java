package it.dieti.dietiestatesbackend.domain.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PasswordResetToken(
        UUID id,
        UUID userId,
        String token,
        OffsetDateTime expiresAt,
        OffsetDateTime consumedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

