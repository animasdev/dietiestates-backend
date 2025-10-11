package it.dieti.dietiestatesbackend.domain.media.listing;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ListingMedia(
        UUID id,
        UUID listingId,
        UUID mediaId,
        Integer sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
