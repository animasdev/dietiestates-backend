package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ListingJpaRepository extends JpaRepository<ListingEntity, UUID> {
    List<ListingEntity> findAllByStatus_CodeAndPendingDeleteUntilBefore(String statusCode, OffsetDateTime threshold);
}
