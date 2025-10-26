package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing;

import it.dieti.dietiestatesbackend.domain.listing.Listing;

final class ListingEntityMapper {

    private ListingEntityMapper() {
    }

    static Listing toDomain(ListingEntity entity) {
        if (entity == null) {
            return null;
        }
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
                entity.getContractDescription(),
                entity.getSecurityDepositCents() != null ? entity.getSecurityDepositCents() : 0L,
                entity.isFurnished(),
                entity.getCondoFeeCents() != null ? entity.getCondoFeeCents() : 0L,
                entity.isPetsAllowed(),
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
