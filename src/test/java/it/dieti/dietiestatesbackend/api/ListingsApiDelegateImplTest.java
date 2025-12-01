package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.application.exception.NotFoundException;
import it.dieti.dietiestatesbackend.application.feature.FeatureService;
import it.dieti.dietiestatesbackend.application.listing.ListingCreationService;
import it.dieti.dietiestatesbackend.application.media.listing.ListingMediaService;
import it.dieti.dietiestatesbackend.application.user.UserProfileService;
import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingType;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingsApiDelegateImplTest {

    @Mock
    private ListingCreationService listingCreationService;
    @Mock
    private ListingMediaService listingMediaService;
    @Mock
    private FeatureService featureService;

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private ListingsApiDelegateImpl delegate;

    @Test
    void listingsIdGet_returnsListingWithPhotos() {
        var listingId = UUID.randomUUID();
        var agencyId = UUID.randomUUID();
        var agentId = UUID.randomUUID();
        var listingTypeId = UUID.randomUUID();
        var statusId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        var listing = new Listing(
                listingId,
                agencyId,
                agentId,
                listingTypeId,
                statusId,
                "Loft centrale",
                "Ampio open space",
                250000L,
                "EUR",
                BigDecimal.valueOf(120),
                3,
                2,
                "A2",
                "Contratto",
                0L,
                false,
                0L,
                true,
                "Via Roma 10",
                "Roma",
                "00100",
                null,
                null,
                null,
                now,
                now,
                now
        );

        var listingType = new ListingType(listingTypeId, "SALE", "Vendita");
        var listingStatus = new ListingStatus(statusId, "PUBLISHED", "Pubblicato", 1, now);

        when(listingCreationService.getListingDetails(listingId,null))
                .thenReturn(new ListingCreationService.ListingDetails(listing, listingType, listingStatus));

        when(listingMediaService.getListingPhotos(listingId)).thenReturn(List.of(
                new ListingMediaService.ListingPhotoView(UUID.randomUUID(), "https://cdn.test/photo.jpg", 1)
        ));

        ResponseEntity<it.dieti.dietiestatesbackend.api.model.Listing> response = delegate.listingsIdGet(listingId);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(listingId);
        assertThat(body.getAgencyId()).isEqualTo(agencyId);
        assertThat(body.getListingType()).isEqualTo(it.dieti.dietiestatesbackend.api.model.Listing.ListingTypeEnum.SALE);
        assertThat(body.getStatus()).isEqualTo(it.dieti.dietiestatesbackend.api.model.Listing.StatusEnum.PUBLISHED);
        assertThat(body.getContractDescription()).isEqualTo("Contratto");
        assertThat(body.getSecurityDepositCents()).isZero();
        assertThat(body.getFurnished()).isFalse();
        assertThat(body.getCondoFeeCents()).isZero();
        assertThat(body.getPetsAllowed()).isTrue();
        assertThat(body.getPhotos()).hasSize(1);
        assertThat(body.getPhotos().get(0).getUrl().toString()).hasToString("https://cdn.test/photo.jpg");
        assertThat(body.getPhotos().get(0).getPosition()).isEqualTo(1);
    }

    @Test
    void listingsIdGet_whenListingMissing_throwsNotFound() {
        var listingId = UUID.randomUUID();
        when(listingCreationService.getListingDetails(listingId,null))
                .thenThrow(NotFoundException.resourceNotFound("Annuncio", listingId));

        assertThatThrownBy(() -> delegate.listingsIdGet(listingId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Annuncio");
    }
}
