package it.dieti.dietiestatesbackend.domain.media;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaAssetCategoryRepository {
    List<MediaAssetCategory> findAll();
    Optional<MediaAssetCategory> findByCode(String code);
    Optional<MediaAssetCategory> findById(UUID id);
}
