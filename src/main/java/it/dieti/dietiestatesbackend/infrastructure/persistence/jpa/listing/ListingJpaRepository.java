package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ListingJpaRepository extends JpaRepository<ListingEntity, UUID> {
}
