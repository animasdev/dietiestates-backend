package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.moderation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ModerationActionJpaRepository extends JpaRepository<ModerationActionEntity, UUID> {
    @EntityGraph(attributePaths = "actionType")
    List<ModerationActionEntity> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "actionType")
    List<ModerationActionEntity> findAllByListing_IdInOrderByCreatedAtDesc(Collection<UUID> listingIds);

    @EntityGraph(attributePaths = "actionType")
    List<ModerationActionEntity> findAllByListing_IdOrderByCreatedAtDesc(UUID listingId);
}
