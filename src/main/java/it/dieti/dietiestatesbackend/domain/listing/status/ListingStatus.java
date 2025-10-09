package it.dieti.dietiestatesbackend.domain.listing.status;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ListingStatus(UUID id, String code, String name, Integer sortOrder, OffsetDateTime createdAt) {}
