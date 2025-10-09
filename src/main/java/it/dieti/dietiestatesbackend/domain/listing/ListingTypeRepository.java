package it.dieti.dietiestatesbackend.domain.listing;

import java.util.Optional;

public interface ListingTypeRepository {
    Optional<ListingType> findByCode(String code);
}
