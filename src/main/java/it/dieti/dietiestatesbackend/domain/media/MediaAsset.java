package it.dieti.dietiestatesbackend.domain.media;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MediaAsset(
        UUID id,
        UUID categoryId,
        String storagePath,
        String publicUrl,
        String mimeType,
        Integer widthPx,
        Integer heightPx,
        UUID createdBy,
        OffsetDateTime createdAt
) {}
