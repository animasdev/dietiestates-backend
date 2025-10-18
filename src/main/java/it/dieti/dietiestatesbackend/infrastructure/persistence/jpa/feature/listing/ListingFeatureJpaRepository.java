package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.feature.listing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingFeatureJpaRepository extends JpaRepository<ListingFeatureEntity, UUID> {
    List<ListingFeatureEntity> findByListing_Id(UUID listingId);
}
