package it.dieti.dietiestatesbackend.domain.listing;

import java.util.Optional;
import java.util.UUID;

public interface ListingRepository {
    Listing save(Listing listing);
    Optional<Listing> findById(UUID id);
}
