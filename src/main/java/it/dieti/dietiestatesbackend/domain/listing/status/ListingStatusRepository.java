package it.dieti.dietiestatesbackend.domain.listing.status;

import java.util.Optional;

public interface ListingStatusRepository {
    Optional<ListingStatus> findByCode(String code);
}
