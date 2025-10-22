package it.dieti.dietiestatesbackend.domain.listing;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository {
    Listing save(Listing listing);
    Optional<Listing> findById(UUID id);
    List<Listing> findPendingDeleteBefore(OffsetDateTime threshold);
}
