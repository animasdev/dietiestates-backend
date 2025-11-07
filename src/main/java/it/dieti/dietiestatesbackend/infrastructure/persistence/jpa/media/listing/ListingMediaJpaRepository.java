package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media.listing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingMediaJpaRepository extends JpaRepository<ListingMediaEntity, UUID> {

    List<ListingMediaEntity> findAllByListing_IdOrderBySortOrderAsc(UUID listingId);

    List<ListingMediaEntity> findAllByMediaIdOrderBySortOrderAsc(UUID mediaId);

    Optional<ListingMediaEntity> findFirstByListing_IdOrderBySortOrderDesc(UUID listingId);

    @Modifying
    @Query("update ListingMediaEntity lm set lm.sortOrder = lm.sortOrder + :offset where lm.listing.id = :listingId")
    int bumpAllByListing(@Param("listingId") UUID listingId, @Param("offset") int offset);
}
