package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MediaAssetJpaRepository extends JpaRepository<MediaAssetEntity, UUID> {
}
