package it.dieti.dietiestatesbackend.domain.user.agency;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Agency(
        UUID id,
        String name,
        String description,
        UUID userId,
        UUID logoMediaId,
        UUID approvedBy,
        OffsetDateTime approvedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
