package it.dieti.dietiestatesbackend.application.listing;

import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingRepository;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusRepository;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusesEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(value = "app.listings.deletion-scheduler.enabled", havingValue = "true", matchIfMissing = true)
class ListingDeletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ListingDeletionScheduler.class);

    private final ListingRepository listingRepository;
    private final ListingStatusRepository listingStatusRepository;

    ListingDeletionScheduler(
            ListingRepository listingRepository,
            ListingStatusRepository listingStatusRepository
    ) {
        this.listingRepository = listingRepository;
        this.listingStatusRepository = listingStatusRepository;
    }

    @Scheduled(fixedDelayString = "PT30M")
    @Transactional
    public void finalizePendingDeletions() {
        var now = OffsetDateTime.now();
        var listingsToDelete = listingRepository.findPendingDeleteBefore(now);
        if (listingsToDelete.isEmpty()) {
            log.debug("No listings pending deletion at {}", now);
            return;
        }

        var deletedStatus = listingStatusRepository.findByCode(ListingStatusesEnum.DELETED.getDescription())
                .orElseThrow(() -> new IllegalStateException("Listing status DELETED not configured"));

        log.info("Finalizing deletion for {} listings pending since <= {}", listingsToDelete.size(), now);
        List<Listing> updatedListings = listingsToDelete.stream()
                .map(listing -> toDeletedListing(listing, deletedStatus.id()))
                .toList();

        updatedListings.forEach(listingRepository::save);
    }

    private Listing toDeletedListing(Listing listing, java.util.UUID deletedStatusId) {
        var processedAt = OffsetDateTime.now();
        return new Listing(
                listing.id(),
                listing.agencyId(),
                listing.ownerAgentId(),
                listing.listingTypeId(),
                deletedStatusId,
                listing.title(),
                listing.description(),
                listing.priceCents(),
                listing.currency(),
                listing.sizeSqm(),
                listing.rooms(),
                listing.floor(),
                listing.energyClass(),
                listing.contractDescription(),
                listing.securityDepositCents(),
                listing.furnished(),
                listing.condoFeeCents(),
                listing.petsAllowed(),
                listing.addressLine(),
                listing.city(),
                listing.postalCode(),
                listing.geo(),
                null,
                processedAt,
                listing.publishedAt(),
                listing.createdAt(),
                processedAt
        );
    }
}
