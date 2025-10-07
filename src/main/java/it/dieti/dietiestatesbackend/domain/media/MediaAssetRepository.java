package it.dieti.dietiestatesbackend.domain.media;

import java.util.Optional;
import java.util.UUID;

public interface MediaAssetRepository {
    Optional<MediaAsset> findById(UUID id);
    MediaAsset save(MediaAsset asset);
    void deleteById(UUID id);
}
