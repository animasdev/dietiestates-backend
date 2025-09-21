package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.feature;

import it.dieti.dietiestatesbackend.domain.feature.Feature;
import it.dieti.dietiestatesbackend.domain.feature.FeatureRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class FeatureRepositoryJpaAdapter implements FeatureRepository {
    private final FeatureJpaRepository jpaRepository;
    public FeatureRepositoryJpaAdapter(FeatureJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    @Override
    public List<Feature> findAll() {
        return jpaRepository.findAll(Sort.by("code").ascending()).stream().map(this::toDomain).toList();
    }
    @Override
    public Optional<Feature> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }

    private Feature toDomain(FeatureEntity e) {
        return new Feature(e.getId(),e.getCode(), e.getName());
    }
}
