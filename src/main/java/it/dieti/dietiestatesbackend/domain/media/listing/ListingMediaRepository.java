package it.dieti.dietiestatesbackend.domain.media.listing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingMediaRepository {
    ListingMedia save(ListingMedia listingMedia);
    Optional<ListingMedia> findById(UUID id);
    List<ListingMedia> findByListingId(UUID listingId);
    List<ListingMedia> findByMediaId(UUID mediaId);
    Integer findNextOrderByListingId(UUID listingId);
    void delete(UUID id);
}
