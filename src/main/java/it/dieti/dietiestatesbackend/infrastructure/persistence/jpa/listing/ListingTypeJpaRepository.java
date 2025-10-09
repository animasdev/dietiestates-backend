package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ListingTypeJpaRepository extends JpaRepository<ListingTypeEntity, UUID> {
    Optional<ListingTypeEntity> findByCode(String code);
}
