package it.dieti.dietiestatesbackend.domain.user;

import java.time.OffsetDateTime;
import java.util.UUID;

public record User(
        UUID id,
        String displayName,
        String email,
        boolean firstAccess,
        UUID roleId,
        String passwordHash,
        String passwordAlgo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}