package it.dieti.dietiestatesbackend.domain.feature.listing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingFeatureRepository {
    ListingFeature save(ListingFeature listingFeature);
    Optional<ListingFeature> findById(UUID id);
    List<ListingFeature> findByListingId(UUID listingId);
    void deleteById(UUID id);
}
