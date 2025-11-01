package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModerationActionTypeJpaRepository extends JpaRepository<ModerationActionTypeEntity, UUID> {
    Optional<ModerationActionTypeEntity> findByCode(String code);
}
