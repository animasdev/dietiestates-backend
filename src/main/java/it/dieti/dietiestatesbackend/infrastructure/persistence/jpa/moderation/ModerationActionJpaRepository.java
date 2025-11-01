package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ModerationActionJpaRepository extends JpaRepository<ModerationActionEntity, UUID> {
    List<ModerationActionEntity> findAllByOrderByCreatedAtDesc();

    List<ModerationActionEntity> findAllByListing_IdInOrderByCreatedAtDesc(Collection<UUID> listingIds);

    List<ModerationActionEntity> findAllByListing_IdOrderByCreatedAtDesc(UUID listingId);
}
