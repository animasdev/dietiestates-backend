package it.dieti.dietiestatesbackend.domain.user.agent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Agent(
        UUID id,
        UUID userId,
        UUID agencyId,
        String reaNumber,
        UUID profilePhotoMediaId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
