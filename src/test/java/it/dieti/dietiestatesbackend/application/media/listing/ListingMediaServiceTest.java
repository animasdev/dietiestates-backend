package it.dieti.dietiestatesbackend.application.media.listing;

import it.dieti.dietiestatesbackend.api.model.ListingPhoto;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.media.MediaAssetService;
import it.dieti.dietiestatesbackend.domain.user.agent.Agent;
import it.dieti.dietiestatesbackend.domain.user.agent.AgentRepository;
import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingRepository;
import it.dieti.dietiestatesbackend.domain.media.MediaAsset;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetRepository;
import it.dieti.dietiestatesbackend.domain.media.listing.ListingMedia;
import it.dieti.dietiestatesbackend.domain.media.listing.ListingMediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingMediaServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private MediaAssetService mediaAssetService;

    @Mock
    private ListingMediaRepository listingMediaRepository;

    @Mock
    private MediaAssetRepository mediaAssetRepository;

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private ListingMediaService service;

    private UUID userId;
    private UUID listingId;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        listingId = UUID.randomUUID();
        agentId = UUID.randomUUID();
    }

    @Test
    void attachListingPhotos_success_persistsAndReturnsViews() {
        // Arrange ownership and listing
        mockAgentOwnerRelationship();
        var photoAssetId1 = UUID.randomUUID();
        var photoAssetId2 = UUID.randomUUID();

        // Media assets looked up by id
        when(mediaAssetService.getListingPhoto(photoAssetId1)).thenReturn(
                new MediaAsset(photoAssetId1, UUID.randomUUID(), "p/1.jpg", "https://cdn/p1.jpg", "image/jpeg", 1000, 800, userId, OffsetDateTime.now())
        );
        when(mediaAssetService.getListingPhoto(photoAssetId2)).thenReturn(
                new MediaAsset(photoAssetId2, UUID.randomUUID(), "p/2.jpg", "https://cdn/p2.jpg", "image/jpeg", 1200, 900, userId, OffsetDateTime.now())
        );

        // Save returns IDs for the join rows
        when(listingMediaRepository.save(any(ListingMedia.class)))
                .thenAnswer(invocation -> {
                    ListingMedia lm = invocation.getArgument(0);
                    return new ListingMedia(UUID.randomUUID(), lm.listingId(), lm.mediaId(), lm.sortOrder(), OffsetDateTime.now(), OffsetDateTime.now());
                });

        var photos = List.of(
                new ListingPhoto().id(photoAssetId1).position(2),
                new ListingPhoto().id(photoAssetId2).position(1)
        );

        // Act
        var added = service.attachListingPhotos(userId, listingId, photos);

        // Assert: URLs and positions returned as in request, order preserved
        assertThat(added).hasSize(2);
        assertThat(added.get(0).getUrl().toString()).hasToString("https://cdn/p1.jpg");
        assertThat(added.get(0).getPosition()).isEqualTo(2);
        assertThat(added.get(1).getUrl().toString()).hasToString("https://cdn/p2.jpg");
        assertThat(added.get(1).getPosition()).isEqualTo(1);

        verify(mediaAssetService, times(1)).getListingPhoto(photoAssetId1);
        verify(mediaAssetService, times(1)).getListingPhoto(photoAssetId2);
        verify(listingMediaRepository, times(2)).save(any(ListingMedia.class));
    }

    @Test
    void attachListingPhotos_whenPositionLessThanOne_throwsBadRequest() {
        mockAgentOwnerRelationship();

        var photosInvalidPos = List.of(new ListingPhoto().id(UUID.randomUUID()).position(0));
        assertThatThrownBy(() -> service.attachListingPhotos(userId, listingId, photosInvalidPos))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Le posizioni devono essere valori interi positivi");

        verify(mediaAssetService, never()).getListingPhoto(any());
        verify(listingMediaRepository, never()).save(any());
    }

    @Test
    void attachListingPhotos_whenDuplicatePositionsInPayload_throwsBadRequest() {
        mockAgentOwnerRelationship();

        var photosDuplicatePos = List.of(
                new ListingPhoto().id(UUID.randomUUID()).position(2),
                new ListingPhoto().id(UUID.randomUUID()).position(2)
        );
        assertThatThrownBy(() -> service.attachListingPhotos(userId, listingId, photosDuplicatePos))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Le posizioni nella richiesta devono essere univoche");

        verify(mediaAssetService, never()).getListingPhoto(any());
        verify(listingMediaRepository, never()).save(any());
    }

    @Test
    void attachListingPhotos_whenPositionsAlreadyTaken_throwsBadRequest() {
        mockAgentOwnerRelationship();
        var existing = new ListingMedia(UUID.randomUUID(), listingId, UUID.randomUUID(), 3, OffsetDateTime.now(), OffsetDateTime.now());
        when(listingMediaRepository.findByListingId(listingId)).thenReturn(List.of(existing));

        var photosTakenPos = List.of(new ListingPhoto().id(UUID.randomUUID()).position(3));
        assertThatThrownBy(() -> service.attachListingPhotos(userId, listingId, photosTakenPos))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Posizione già assegnata");

        verify(mediaAssetService, never()).getListingPhoto(any());
        verify(listingMediaRepository, never()).save(any());
    }

    @Test
    void getListingPhotos_returnsViewModels() {
        var mediaId = UUID.randomUUID();
        var listingMedia = new ListingMedia(UUID.randomUUID(), listingId, mediaId, 2, OffsetDateTime.now(), OffsetDateTime.now());
        when(listingMediaRepository.findByListingId(listingId)).thenReturn(List.of(listingMedia));
        when(mediaAssetRepository.findById(mediaId)).thenReturn(Optional.of(
                new MediaAsset(mediaId, UUID.randomUUID(), "path", "https://cdn.test/photo.jpg", "image/jpeg", 800, 600, userId, OffsetDateTime.now())
        ));

        var views = service.getListingPhotos(listingId);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).publicUrl()).isEqualTo("https://cdn.test/photo.jpg");
        assertThat(views.get(0).position()).isEqualTo(2);
    }

    @Test
    void getListingPhotos_skipsMissingAssets() {
        var listingMedia = new ListingMedia(UUID.randomUUID(), listingId, UUID.randomUUID(), 1, OffsetDateTime.now(), OffsetDateTime.now());
        when(listingMediaRepository.findByListingId(listingId)).thenReturn(List.of(listingMedia));
        when(mediaAssetRepository.findById(any())).thenReturn(Optional.empty());

        var views = service.getListingPhotos(listingId);

        assertThat(views).isEmpty();
    }

    private void mockAgentOwnerRelationship() {
        var agent = new Agent(agentId, userId, UUID.randomUUID(), "REA123", null, OffsetDateTime.now(), OffsetDateTime.now());
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));

        var listing = new Listing(
                listingId,
                UUID.randomUUID(),
                agent.id(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "title",
                "description",
                1000L,
                "EUR",
                BigDecimal.TEN,
                3,
                1,
                "A2",
                null,
                0L,
                false,
                0L,
                false,
                "address",
                "city",
                "12345",
                null,
                null,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
    }
}
