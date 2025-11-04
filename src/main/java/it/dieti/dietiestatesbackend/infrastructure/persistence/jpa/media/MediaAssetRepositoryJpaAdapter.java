package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media;

import it.dieti.dietiestatesbackend.domain.media.MediaAsset;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetRepository;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.UserEntity;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MediaAssetRepositoryJpaAdapter implements MediaAssetRepository {
    private final MediaAssetJpaRepository jpaRepository;

    public MediaAssetRepositoryJpaAdapter(MediaAssetJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<MediaAsset> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public MediaAsset getByIdAndCategory(UUID id, UUID categoryId) {
        var entity = jpaRepository
                .findByIdAndCategory_Id(id, categoryId)
                .orElseThrow(() -> new NoSuchElementException("Nessun media asset con id '" + id + "' e category-id '" + categoryId + "'"));
        return toDomain(entity);
    }

    @Override
    public MediaAsset save(MediaAsset asset) {
        MediaAssetEntity entity;
        if (asset.id() != null) {
            entity = jpaRepository.findById(asset.id())
                    .orElseThrow(() -> new IllegalArgumentException("Media asset not found: " + asset.id()));
        } else {
            entity = new MediaAssetEntity();
            entity.setCreatedAt(asset.createdAt() != null ? asset.createdAt() : OffsetDateTime.now());
            if (asset.createdBy() == null) {
                throw new IllegalArgumentException("createdBy is required for new media assets");
            }
            var createdByRef = new UserEntity();
            createdByRef.setId(asset.createdBy());
            entity.setCreatedBy(createdByRef);
        }

        if (asset.categoryId() == null) {
            throw new IllegalArgumentException("categoryId is required for media assets");
        }
        var categoryRef = new MediaAssetCategoryEntity();
        categoryRef.setId(asset.categoryId());
        entity.setCategory(categoryRef);

        entity.setStoragePath(asset.storagePath());
        entity.setPublicUrl(asset.publicUrl());
        entity.setMimeType(asset.mimeType());
        entity.setWidthPx(asset.widthPx());
        entity.setHeightPx(asset.heightPx());

        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private MediaAsset toDomain(MediaAssetEntity entity) {
        return new MediaAsset(
                entity.getId(),
                entity.getCategory().getId(),
                entity.getStoragePath(),
                entity.getPublicUrl(),
                entity.getMimeType(),
                entity.getWidthPx(),
                entity.getHeightPx(),
                entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null,
                entity.getCreatedAt()
        );
    }
}
