package it.dieti.dietiestatesbackend.domain.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RefreshToken(
        UUID id,
        UUID userId,
        String tokenHash,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

