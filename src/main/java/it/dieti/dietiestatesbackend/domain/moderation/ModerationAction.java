package it.dieti.dietiestatesbackend.domain.moderation;

import it.dieti.dietiestatesbackend.domain.user.role.RolesEnum;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ModerationAction(
        UUID id,
        UUID listingId,
        UUID performedByUserId,
        RolesEnum performedByRole,
        ModerationActionType actionType,
        String reason,
        OffsetDateTime createdAt
) {}
