package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media.listing;

import it.dieti.dietiestatesbackend.domain.media.listing.ListingMedia;
import it.dieti.dietiestatesbackend.domain.media.listing.ListingMediaRepository;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing.ListingEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media.MediaAssetEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ListingMediaRepositoryJpaAdapter implements ListingMediaRepository {

    private final ListingMediaJpaRepository repository;

    public ListingMediaRepositoryJpaAdapter(ListingMediaJpaRepository repository) {
        this.repository = repository;
    }


    @Override
    public ListingMedia save(ListingMedia listingMedia) {
        ListingMediaEntity entity;

        if (listingMedia.id() != null) {
            entity = repository
                    .findById(listingMedia.id())
                    .orElseThrow(() -> new IllegalArgumentException("ListingMedia not found: " + listingMedia.id()));
        } else {
            entity = new ListingMediaEntity();
            var createdAt = listingMedia.createdAt() != null ? listingMedia.createdAt() : OffsetDateTime.now();
            entity.setCreatedAt(createdAt);
        }
        var listingRef = new ListingEntity();
        listingRef.setId(listingMedia.listingId());
        entity.setListing(listingRef);
        var mediaRef = new MediaAssetEntity();
        mediaRef.setId(listingMedia.mediaId());
        entity.setMedia(mediaRef);
        entity.setSortOrder(listingMedia.sortOrder());
        var updatedAt = listingMedia.updatedAt() != null ? listingMedia.updatedAt() : OffsetDateTime.now();
        entity.setUpdatedAt(updatedAt);
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ListingMedia> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }


    @Override
    public List<ListingMedia> findByListingId(UUID listingId) {
        return repository.findAllByListing_IdOrderBySortOrderAsc(listingId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Integer findNextOrderByListingId(UUID listingId) {
        return repository.findFirstByListing_IdOrderBySortOrderDesc(listingId)
                .map(entity -> entity.getSortOrder() + 1)
                .orElse(1);
    }


    private ListingMedia toDomain(ListingMediaEntity entity) {
        return new ListingMedia(
                entity.getId(),
                entity.getListing().getId(),
                entity.getMedia().getId(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
