package it.dieti.dietiestatesbackend.domain.listing.search;

import it.dieti.dietiestatesbackend.domain.listing.Listing;

import java.util.List;
import java.util.UUID;

public interface ListingSearchRepository {

    SearchResult search(SearchFilters filters);

    record SearchFilters(
            UUID listingTypeId,
            UUID statusId,
            String normalizedCity,
            Integer minPriceCents,
            Integer maxPriceCents,
            Integer rooms,
            String normalizedEnergyClass,
            List<UUID> featureIds,
            Double latitude,
            Double longitude,
            Integer radiusMeters,
            String sortColumn,
            boolean sortAscending,
            int page,
            int size
    ) {}

    record SearchResult(List<Listing> listings, long total) {}
}

