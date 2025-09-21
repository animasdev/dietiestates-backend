package it.dieti.dietiestatesbackend.domain.feature;

import java.util.List;
import java.util.Optional;

public interface FeatureRepository {
    List<Feature> findAll();
    Optional<Feature> findByCode(String code);
}
