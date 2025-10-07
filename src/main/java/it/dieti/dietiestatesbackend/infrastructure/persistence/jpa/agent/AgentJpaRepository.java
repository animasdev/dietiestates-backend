package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentJpaRepository extends JpaRepository<AgentEntity, UUID> {
    Optional<AgentEntity> findByUserId(UUID userId);
    List<AgentEntity> findByAgencyId(UUID agencyId);
}
