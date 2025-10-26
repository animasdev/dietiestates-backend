package it.dieti.dietiestatesbackend.domain.listing;

import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Listing(
        UUID id,
        UUID agencyId,
        UUID ownerAgentId,
        UUID listingTypeId,
        UUID statusId,
        String title,
        String description,
        long priceCents,
        String currency,
        BigDecimal sizeSqm,
        Integer rooms,
        Integer floor,
        String energyClass,
        String contractDescription,
        long securityDepositCents,
        boolean furnished,
        long condoFeeCents,
        boolean petsAllowed,
        String addressLine,
        String city,
        String postalCode,
        Point geo,
        OffsetDateTime pendingDeleteUntil,
        OffsetDateTime deletedAt,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
