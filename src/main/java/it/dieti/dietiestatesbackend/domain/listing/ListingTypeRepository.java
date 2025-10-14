package it.dieti.dietiestatesbackend.domain.listing;

import java.util.Optional;
import java.util.UUID;

public interface ListingTypeRepository {
    Optional<ListingType> findByCode(String code);
    Optional<ListingType> findById(UUID id);
}
