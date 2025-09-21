package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.feature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeatureJpaRepository  extends JpaRepository<FeatureEntity, UUID> {
    Optional<FeatureEntity> findByCode(String code);
}
