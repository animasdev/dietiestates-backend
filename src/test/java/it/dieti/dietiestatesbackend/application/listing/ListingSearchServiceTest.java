package it.dieti.dietiestatesbackend.application.listing;

import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.feature.FeatureService;
import it.dieti.dietiestatesbackend.application.media.listing.ListingMediaService;
import it.dieti.dietiestatesbackend.application.media.listing.ListingMediaService.ListingPhotoView;
import it.dieti.dietiestatesbackend.domain.feature.Feature;
import it.dieti.dietiestatesbackend.domain.feature.FeatureRepository;
import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingType;
import it.dieti.dietiestatesbackend.domain.listing.ListingTypeRepository;
import it.dieti.dietiestatesbackend.domain.listing.search.ListingSearchRepository;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatus;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingSearchServiceTest {

    @Mock
    private ListingSearchRepository listingSearchRepository;
    @Mock
    private ListingTypeRepository listingTypeRepository;
    @Mock
    private ListingStatusRepository listingStatusRepository;
    @Mock
    private FeatureRepository featureRepository;
    @Mock
    private FeatureService featureService;
    @Mock
    private ListingMediaService listingMediaService;
    @org.mockito.Spy
    private CoordinatesValidator coordinatesValidator;

    @InjectMocks
    private ListingSearchService service;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    void search_enforcesPublishedStatusForPublicRequests() {
        var listingTypeId = UUID.randomUUID();
        var statusId = UUID.randomUUID();
        var listingId = UUID.randomUUID();
        var agencyId = UUID.randomUUID();
        var agentId = UUID.randomUUID();
        var featureId = UUID.randomUUID();

        var listingType = new ListingType(listingTypeId, "SALE", "Vendita");
        var publishedStatus = new ListingStatus(statusId, "PUBLISHED", "Pubblicato", 1, OffsetDateTime.now());
        var feature = new Feature(featureId, "ELEVATOR", "Ascensore");

        when(listingTypeRepository.findByCode("SALE")).thenReturn(Optional.of(listingType));
        when(listingStatusRepository.findByCode("PUBLISHED")).thenReturn(Optional.of(publishedStatus));
        when(featureRepository.findByCode("ELEVATOR")).thenReturn(Optional.of(feature));

        var now = OffsetDateTime.now();
        var listing = new Listing(
                listingId,
                agencyId,
                agentId,
                listingTypeId,
                statusId,
                "Attico",
                "Vista mare",
                250_000L,
                "EUR",
                BigDecimal.valueOf(120),
                4,
                2,
                "A2",
                null,
                0L,
                false,
                0L,
                false,
                "Via Roma 10",
                "Roma",
                "00100",
                GEOMETRY_FACTORY.createPoint(new Coordinate(12.5, 41.9)),
                null,
                null,
                now,
                now,
                now
        );

        var repositoryResult = new ListingSearchRepository.SearchResult(List.of(listing), 1L);
        when(listingSearchRepository.search(any())).thenReturn(repositoryResult);
        when(listingTypeRepository.findById(listingTypeId)).thenReturn(Optional.of(listingType));
        when(listingStatusRepository.findById(statusId)).thenReturn(Optional.of(publishedStatus));
        when(featureService.getListingFeatures(listingId)).thenReturn(List.of(feature));
        when(listingMediaService.getListingPhotos(listingId)).thenReturn(List.of(new ListingPhotoView(UUID.randomUUID(), "https://cdn/photo.jpg", 1)));

        var query = new ListingSearchService.SearchQuery(
                "SALE",            // type
                "Roma",            // city
                100_000,           // minPrice
                300_000,           // maxPrice
                3,                 // minRooms
                null,              // maxRooms
                null,              // minSqm
                null,              // maxSqm
                List.of("A2"),     // energyClasses
                List.of("ELEVATOR"), // features
                null,              // postalCodes
                "DRAFT",           // status
                41.9,              // latitude
                12.5,              // longitude
                1_000,             // radiusMeters
                null,              // hasPhotos
                null,              // furnished
                null,              // petsAllowed
                null,              // agencyId
                null,              // ownerAgentId
                0,                 // page
                10,                // size
                "priceCents,asc",  // sort
                true               // enforcePublishedOnly
        );

        var result = service.search(query);

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).hasSize(1);
        var item = result.items().getFirst();
        assertThat(item.listing()).isEqualTo(listing);
        assertThat(item.listingType()).isEqualTo(listingType);
        assertThat(item.listingStatus()).isEqualTo(publishedStatus);
        assertThat(item.features()).containsExactly(feature);
        assertThat(item.photos()).hasSize(1);

        var filtersCaptor = ArgumentCaptor.forClass(ListingSearchRepository.SearchFilters.class);
        verify(listingSearchRepository).search(filtersCaptor.capture());
        var filters = filtersCaptor.getValue();
        assertThat(filters.statusId()).isEqualTo(statusId);
        assertThat(filters.sortColumn()).isEqualTo("price_cents");
        assertThat(filters.sortAscending()).isTrue();
        assertThat(filters.hasPhotos()).isNull();
        assertThat(filters.agencyId()).isNull();
        assertThat(filters.ownerAgentId()).isNull();
    }

    @Test
    void search_whenHasPhotosTrue_appliesFilter() {
        var repositoryResult = new ListingSearchRepository.SearchResult(List.of(), 0);
        when(listingSearchRepository.search(any())).thenReturn(repositoryResult);

        var query = new ListingSearchService.SearchQuery(
                null,   // type
                null,   // city
                null,   // minPrice
                null,   // maxPrice
                null,   // minRooms
                null,   // maxRooms
                null,   // minSqm
                null,   // maxSqm
                null,   // energyClasses
                null,   // features
                null,   // postalCodes
                null,   // status
                null,   // latitude
                null,   // longitude
                null,   // radiusMeters
                true,   // hasPhotos
                null,   // furnished
                null,   // petsAllowed
                null,   // agencyId
                null,   // ownerAgentId
                0,      // page
                20,     // size
                null,   // sort
                false   // enforcePublishedOnly
        );

        service.search(query);

        var filtersCaptor = ArgumentCaptor.forClass(ListingSearchRepository.SearchFilters.class);
        verify(listingSearchRepository).search(filtersCaptor.capture());
        assertThat(filtersCaptor.getValue().hasPhotos()).isTrue();
    }

    @Test
    void search_whenAgencyAndOwnerProvided_applyFilters() {
        var repositoryResult = new ListingSearchRepository.SearchResult(List.of(), 0);
        when(listingSearchRepository.search(any())).thenReturn(repositoryResult);

        var agencyId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        var query = new ListingSearchService.SearchQuery(
                null,       // type
                null,       // city
                null,       // minPrice
                null,       // maxPrice
                null,       // minRooms
                null,       // maxRooms
                null,       // minSqm
                null,       // maxSqm
                null,       // energyClasses
                null,       // features
                null,       // postalCodes
                null,       // status
                null,       // latitude
                null,       // longitude
                null,       // radiusMeters
                null,       // hasPhotos
                null,       // furnished
                null,       // petsAllowed
                agencyId,   // agencyId
                ownerId,    // ownerAgentId
                0,          // page
                20,         // size
                null,       // sort
                false       // enforcePublishedOnly
        );

        service.search(query);

        var filtersCaptor = ArgumentCaptor.forClass(ListingSearchRepository.SearchFilters.class);
        verify(listingSearchRepository).search(filtersCaptor.capture());
        var filters = filtersCaptor.getValue();
        assertThat(filters.agencyId()).isEqualTo(agencyId);
        assertThat(filters.ownerAgentId()).isEqualTo(ownerId);
    }

    @Test
    void search_whenSortFieldInvalid_throwsBadRequest() {
        var query = new ListingSearchService.SearchQuery(
                null,   // type
                null,   // city
                null,   // minPrice
                null,   // maxPrice
                null,   // minRooms
                null,   // maxRooms
                null,   // minSqm
                null,   // maxSqm
                null,   // energyClasses
                null,   // features
                null,   // postalCodes
                null,   // status
                null,   // latitude
                null,   // longitude
                null,   // radiusMeters
                null,   // hasPhotos
                null,   // furnished
                null,   // petsAllowed
                null,   // agencyId
                null,   // ownerAgentId
                0,      // page
                20,     // size
                "invalid,asc",
                false
        );

        assertThatThrownBy(() -> service.search(query))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Campo di ordinamento non supportato");
    }
}
