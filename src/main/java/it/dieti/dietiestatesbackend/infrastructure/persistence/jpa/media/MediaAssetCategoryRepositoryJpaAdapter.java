package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media;

import it.dieti.dietiestatesbackend.domain.media.MediaAssetCategory;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetCategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MediaAssetCategoryRepositoryJpaAdapter implements MediaAssetCategoryRepository {
    private final MediaAssetCategoryJpaRepository jpaRepository;

    public MediaAssetCategoryRepositoryJpaAdapter(MediaAssetCategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<MediaAssetCategory> findAll() {
        return jpaRepository.findAll(Sort.by("code").ascending()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<MediaAssetCategory> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }

    @Override
    public Optional<MediaAssetCategory> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private MediaAssetCategory toDomain(MediaAssetCategoryEntity entity) {
        return new MediaAssetCategory(entity.getId(), entity.getCode(), entity.getDescription());
    }
}
