package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing;

import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingRepository;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.agent.AgentEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.agency.AgencyEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing.ListingStatusEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing.ListingTypeEntity;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ListingRepositoryJpaAdapter implements ListingRepository {
    private final ListingJpaRepository jpaRepository;

    public ListingRepositoryJpaAdapter(ListingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Listing save(Listing listing) {
        ListingEntity entity;
        if (listing.id() != null) {
            entity = jpaRepository.findById(listing.id())
                    .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + listing.id()));
        } else {
            entity = new ListingEntity();
            entity.setCreatedAt(OffsetDateTime.now());
        }

        var agencyRef = new AgencyEntity();
        agencyRef.setId(listing.agencyId());
        entity.setAgency(agencyRef);

        var ownerRef = new AgentEntity();
        ownerRef.setId(listing.ownerAgentId());
        entity.setOwnerAgent(ownerRef);

        var typeRef = new ListingTypeEntity();
        typeRef.setId(listing.listingTypeId());
        entity.setListingType(typeRef);

        var statusRef = new ListingStatusEntity();
        statusRef.setId(listing.statusId());
        entity.setStatus(statusRef);

        entity.setTitle(listing.title());
        entity.setDescription(listing.description());
        entity.setPriceCents(listing.priceCents());
        entity.setCurrency(listing.currency());
        entity.setSizeSqm(listing.sizeSqm());
        entity.setRooms(listing.rooms());
        entity.setFloor(listing.floor());
        entity.setEnergyClass(listing.energyClass());
        entity.setAddressLine(listing.addressLine());
        entity.setCity(listing.city());
        entity.setPostalCode(listing.postalCode());
        entity.setGeo(listing.geo());
        entity.setPendingDeleteUntil(listing.pendingDeleteUntil());
        entity.setDeletedAt(listing.deletedAt());
        entity.setPublishedAt(listing.publishedAt());
        entity.setUpdatedAt(OffsetDateTime.now());

        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Listing> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private Listing toDomain(ListingEntity entity) {
        return new Listing(
                entity.getId(),
                entity.getAgency() != null ? entity.getAgency().getId() : null,
                entity.getOwnerAgent() != null ? entity.getOwnerAgent().getId() : null,
                entity.getListingType() != null ? entity.getListingType().getId() : null,
                entity.getStatus() != null ? entity.getStatus().getId() : null,
                entity.getTitle(),
                entity.getDescription(),
                entity.getPriceCents() != null ? entity.getPriceCents() : 0L,
                entity.getCurrency(),
                entity.getSizeSqm(),
                entity.getRooms(),
                entity.getFloor(),
                entity.getEnergyClass(),
                entity.getAddressLine(),
                entity.getCity(),
                entity.getPostalCode(),
                entity.getGeo(),
                entity.getPendingDeleteUntil(),
                entity.getDeletedAt(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
