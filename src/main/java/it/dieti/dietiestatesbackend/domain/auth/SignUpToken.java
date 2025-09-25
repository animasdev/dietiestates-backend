package it.dieti.dietiestatesbackend.domain.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SignUpToken(
        UUID id,
        String email,
        String displayName,
        String token,
        OffsetDateTime expiresAt,
        OffsetDateTime consumedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

