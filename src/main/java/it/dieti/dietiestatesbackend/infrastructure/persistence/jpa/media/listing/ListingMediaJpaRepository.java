package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media.listing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingMediaJpaRepository extends JpaRepository<ListingMediaEntity, UUID> {

    List<ListingMediaEntity> findAllByListing_IdOrderBySortOrderAsc(UUID listingId);

    List<ListingMediaEntity> findAllByMediaIdOrderBySortOrderAsc(UUID mediaId);

    Optional<ListingMediaEntity> findFirstByListing_IdOrderBySortOrderDesc(UUID listingId);
}
