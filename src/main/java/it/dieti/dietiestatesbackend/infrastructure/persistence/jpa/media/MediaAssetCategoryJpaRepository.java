package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MediaAssetCategoryJpaRepository extends JpaRepository<MediaAssetCategoryEntity, UUID> {
    Optional<MediaAssetCategoryEntity> findByCode(String code);
}
