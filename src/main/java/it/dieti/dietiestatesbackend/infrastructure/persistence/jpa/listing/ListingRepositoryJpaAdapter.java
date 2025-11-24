package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing;

import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingRepository;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusesEnum;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.agent.AgentEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.agency.AgencyEntity;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
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
        entity.setContractDescription(listing.contractDescription());
        entity.setSecurityDepositCents(listing.securityDepositCents());
        entity.setFurnished(listing.furnished());
        entity.setCondoFeeCents(listing.condoFeeCents());
        entity.setPetsAllowed(listing.petsAllowed());
        entity.setAddressLine(listing.addressLine());
        entity.setCity(listing.city());
        entity.setPostalCode(listing.postalCode());
        entity.setGeo(listing.geo());
        entity.setPendingDeleteUntil(listing.pendingDeleteUntil());
        entity.setDeletedAt(listing.deletedAt());
        entity.setPublishedAt(listing.publishedAt());
        entity.setUpdatedAt(OffsetDateTime.now());

        var saved = jpaRepository.save(entity);
        return ListingEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Listing> findById(UUID id) {
        return jpaRepository.findById(id).map(ListingEntityMapper::toDomain);
    }

    @Override
    public List<Listing> findAllByOwnerAgentId(UUID ownerAgentId) {
        return jpaRepository.findAllByOwnerAgent_Id(ownerAgentId).stream()
                .map(ListingEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Listing> findAllByAgencyId(UUID agencyId) {
        return jpaRepository.findAllByAgency_Id(agencyId).stream()
                .map(ListingEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Listing> findPendingDeleteBefore(OffsetDateTime threshold) {
        return jpaRepository
                .findAllByStatus_CodeAndPendingDeleteUntilBefore(ListingStatusesEnum.PENDING_DELETE.getDescription(), threshold)
                .stream()
                .map(ListingEntityMapper::toDomain)
                .toList();
    }
}
