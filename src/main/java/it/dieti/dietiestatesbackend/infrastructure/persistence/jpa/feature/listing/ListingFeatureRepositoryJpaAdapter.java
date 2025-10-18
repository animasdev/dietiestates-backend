package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.feature.listing;

import it.dieti.dietiestatesbackend.domain.feature.listing.ListingFeature;
import it.dieti.dietiestatesbackend.domain.feature.listing.ListingFeatureRepository;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.feature.FeatureEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing.ListingEntity;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ListingFeatureRepositoryJpaAdapter implements ListingFeatureRepository {

    private final ListingFeatureJpaRepository repository;

    public ListingFeatureRepositoryJpaAdapter(ListingFeatureJpaRepository repository) {
        this.repository = repository;
    }


    @Override
    public ListingFeature save(ListingFeature listingFeature) {
        ListingFeatureEntity entity;

        if (listingFeature.id() != null) {
            entity = repository.findById(listingFeature.id())
                    .orElseThrow(() -> new IllegalArgumentException("ListingFeature not found: " + listingFeature.id()));
        } else {
            entity = new ListingFeatureEntity();
            var createdAt = listingFeature.createdAt() != null ? listingFeature.createdAt() : OffsetDateTime.now();
            entity.setCreatedAt(createdAt);
        }
        var listingRef = new ListingEntity();
        listingRef.setId(listingFeature.listingId());
        entity.setListing(listingRef);
        var featureRef = new FeatureEntity();
        featureRef.setId(listingFeature.featureId());
        entity.setFeature(featureRef);
        entity.setPriceCents(listingFeature.priceCents());
        var updatedAt = listingFeature.updatedAt() != null ? listingFeature.updatedAt() : OffsetDateTime.now();
        entity.setUpdatedAt(updatedAt);
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    private ListingFeature toDomain(ListingFeatureEntity entity) {
        return new ListingFeature(
                entity.getId(),
                entity.getListing().getId(),
                entity.getFeature().getId(),
                entity.getPriceCents(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    @Override
    public Optional<ListingFeature> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ListingFeature> findByListingId(UUID listingId) {
        return repository.findByListing_Id(listingId).stream().map(this::toDomain).toList();
    }
}
