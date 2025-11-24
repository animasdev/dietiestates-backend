package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.agency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgencyJpaRepository extends JpaRepository<AgencyEntity, UUID> {
    Optional<AgencyEntity> findByUserId(UUID userId);
}
