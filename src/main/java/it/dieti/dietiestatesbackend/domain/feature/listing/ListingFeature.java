package it.dieti.dietiestatesbackend.domain.feature.listing;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ListingFeature(
        UUID id,
        UUID listingId,
        UUID featureId,
        long priceCents,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
