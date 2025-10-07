package it.dieti.dietiestatesbackend.domain.media;

import java.util.UUID;

public record MediaAssetCategory(
        UUID id,
        String code,
        String description
) {}
