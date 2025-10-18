package it.dieti.dietiestatesbackend.domain.feature;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureRepository {
    List<Feature> findAll();
    Optional<Feature> findByCode(String code);
    Optional<Feature> findById(UUID id);
}
