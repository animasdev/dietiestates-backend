package it.dieti.dietiestatesbackend.domain.listing.status;

import java.util.Optional;
import java.util.UUID;

public interface ListingStatusRepository {
    Optional<ListingStatus> findByCode(String code);
    Optional<ListingStatus> findById(UUID id);
}
