package it.dieti.dietiestatesbackend.application.media.listing;

import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingRepository;
import it.dieti.dietiestatesbackend.domain.media.MediaAsset;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetRepository;
import it.dieti.dietiestatesbackend.domain.media.listing.ListingMedia;
import it.dieti.dietiestatesbackend.domain.media.listing.ListingMediaRepository;
import it.dieti.dietiestatesbackend.domain.user.User;
import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import it.dieti.dietiestatesbackend.domain.user.agent.Agent;
import it.dieti.dietiestatesbackend.domain.user.agent.AgentRepository;
import it.dieti.dietiestatesbackend.domain.user.role.Role;
import it.dieti.dietiestatesbackend.domain.user.role.RoleRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RolesEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.DisplayName("Black-box tests: reorderListingPhotos")
class ReorderListingPhotosBlackBoxTests {

    @Mock ListingRepository listingRepository;
    @Mock ListingMediaRepository listingMediaRepository;
    @Mock MediaAssetRepository mediaAssetRepository;
    @Mock AgentRepository agentRepository;
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock it.dieti.dietiestatesbackend.application.media.MediaAssetService mediaAssetService;

    private ListingMediaService service;

    @BeforeEach
    void setUp() {
        service = new ListingMediaService(
                listingRepository,
                mediaAssetService,
                listingMediaRepository,
                mediaAssetRepository,
                agentRepository,
                userRepository,
                roleRepository
        );
    }


    private UUID stubUserWithRole(RolesEnum role) {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User(
                userId, "User", "user@example.com", false, roleId, null, null, null, null, null
        )));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(new Role(roleId, role.name(), role.name(), "")));
        return userId;
    }

    private Listing stubListing(UUID ownerAgentId) {
        UUID listingId = UUID.randomUUID();
        Listing listing = new Listing(
                listingId,
                UUID.randomUUID(),
                ownerAgentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Title",
                "Desc",
                1000,
                "EUR",
                null, null, null,
                null, null,
                0, false, 0, false,
                "addr","city",null,
                null, null, null, null, null, null
        );
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        return listing;
    }

    private static List<ListingMedia> sortedByOrder(List<ListingMedia> list) {
        return list.stream().sorted(Comparator.comparing(ListingMedia::sortOrder)).toList();
    }

    private List<ListingMedia> installInMemoryListingMedia(UUID listingId, List<ListingMedia> initial) {
        List<ListingMedia> store = new ArrayList<>(initial);

        when(listingMediaRepository.findByListingId(listingId)).thenAnswer(inv -> sortedByOrder(store));

        when(listingMediaRepository.save(any())).thenAnswer(inv -> {
            ListingMedia lm = inv.getArgument(0, ListingMedia.class);
            for (int i = 0; i < store.size(); i++) {
                if (store.get(i).id().equals(lm.id())) {
                    store.set(i, lm);
                    break;
                }
            }
            return lm;
        });

        for (ListingMedia lm : initial) {
            when(mediaAssetRepository.findById(lm.mediaId())).thenReturn(Optional.of(
                    new MediaAsset(lm.mediaId(), null, null, "http://cdn/" + lm.mediaId(), "image/jpeg", 100, 100, null, OffsetDateTime.now())
            ));
        }

        return store;
    }

    @Test
    @org.junit.jupiter.api.DisplayName("ADMIN: riordino con successo e posizioni 1..N")
    // Tupla: TU1
    // Copre classi: CE1.B (userId esistente), CE2.A (Privilegiato), CE3.B (listing esistente), CE4.— (bypass ownership), CE5.F (insieme uguale alle correnti)
    void reorder_asAdmin_success_reassignsSequentialPositions() {
        // Arrange
        UUID userId = stubUserWithRole(RolesEnum.ADMIN);
        Agent owner = new Agent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, null, null, null);
        Listing listing = stubListing(owner.id());

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();
        List<ListingMedia> existing = List.of(
                new ListingMedia(p1, listing.id(), UUID.randomUUID(), 10, OffsetDateTime.now(), null),
                new ListingMedia(p2, listing.id(), UUID.randomUUID(), 30, OffsetDateTime.now(), null),
                new ListingMedia(p3, listing.id(), UUID.randomUUID(), 20, OffsetDateTime.now(), null)
        );
        installInMemoryListingMedia(listing.id(), existing);

        List<UUID> ordered = List.of(p3, p1, p2);

        // Act
        var result = service.reorderListingPhotos(userId, listing.id(), ordered);

        // Assert: output riflette il nuovo ordine (black-box)
        int size = result.size();
        List<Integer> positions = result.stream().map(ListingMediaService.ListingPhotoView::position).toList();
        List<UUID> ids = result.stream().map(ListingMediaService.ListingPhotoView::id).toList();
        assertEquals(3, size);
        assertEquals(List.of(1, 2, 3), positions);
        assertEquals(List.of(p3, p1, p2), ids);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AGENT proprietario: riordino con successo")
    // Tupla: TU2
    // Copre classi: CE1.B, CE2.B (Agente), CE3.B, CE4.A (owner=True), CE5.F
    void reorder_asAgentOwner_success() {
        // Arrange
        UUID userId = stubUserWithRole(RolesEnum.AGENT);
        Agent owner = new Agent(UUID.randomUUID(), userId, UUID.randomUUID(), null, null, null, null);
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(owner));
        Listing listing = stubListing(owner.id());

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        installInMemoryListingMedia(listing.id(), List.of(
                new ListingMedia(p1, listing.id(), UUID.randomUUID(), 1, OffsetDateTime.now(), null),
                new ListingMedia(p2, listing.id(), UUID.randomUUID(), 2, OffsetDateTime.now(), null)
        ));

        // Act
        var result = service.reorderListingPhotos(userId, listing.id(), List.of(p2, p1));
        // Assert
        List<UUID> ids = result.stream().map(ListingMediaService.ListingPhotoView::id).toList();
        assertEquals(List.of(p2, p1), ids);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AGENT non proprietario: Forbidden")
    // Tupla: TU3
    // Copre classi: CE1.B, CE2.B, CE3.B, CE4.B (owner=False), CE5.DC
    void reorder_agentNotOwner_forbidden() {
        // Arrange
        UUID userId = stubUserWithRole(RolesEnum.AGENT);
        Agent agent = new Agent(UUID.randomUUID(), userId, UUID.randomUUID(), null, null, null, null);
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));

        Listing listing = stubListing(UUID.randomUUID()); // different owner
        when(listingRepository.findById(listing.id())).thenReturn(Optional.of(listing));

        // Act + Assert
        UUID photoId = UUID.randomUUID();
        List<UUID> ordered = List.of(photoId);
        UUID listingId = listing.id();
        assertThrows(ForbiddenException.class, () -> service.reorderListingPhotos(userId, listingId, ordered));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Input non valido: orderedPhotoIds null -> BadRequest")
    // Tupla: TU5
    // Copre classi: CE1.B, CE2.A, CE3.B, CE4.—, CE5.A (NULL)
    void reorder_nullList_badRequest() {
        // Arrange
        UUID userId = stubUserWithRole(RolesEnum.ADMIN);
        Listing listing = stubListing(UUID.randomUUID());
        // Act + Assert
        UUID listingId = listing.id();
        assertThrows(BadRequestException.class, () -> service.reorderListingPhotos(userId, listingId, null));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Input non valido: orderedPhotoIds vuota -> BadRequest")
    // Tupla: TU6
    // Copre classi: CE1.B, CE2.A, CE3.B, CE4.—, CE5.B (lista vuota)
    void reorder_emptyList_badRequest() {
        // Arrange
        UUID userId = stubUserWithRole(RolesEnum.ADMIN);
        Listing listing = stubListing(UUID.randomUUID());
        // Act + Assert
        List<UUID> ordered = List.of();
        UUID listingId = listing.id();
        assertThrows(BadRequestException.class, () -> service.reorderListingPhotos(userId, listingId, ordered));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Boundary: nessuna foto esistente -> BadRequest dedicato")
    // Nota: test di confine non parte della suite di tuple richiesta; mantenuto per completezza.
    void reorder_noExistingPhotos_badRequest() {
        // Arrange
        UUID userId = stubUserWithRole(RolesEnum.ADMIN);
        Listing listing = stubListing(UUID.randomUUID());
        when(listingMediaRepository.findByListingId(listing.id())).thenReturn(List.of());
        // Act + Assert
        UUID photoId = UUID.randomUUID();
        List<UUID> ordered = List.of(photoId);
        UUID listingId = listing.id();
        assertThrows(BadRequestException.class, () -> service.reorderListingPhotos(userId, listingId, ordered));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Input non valido: cardinalità diversa da N -> BadRequest")
    // Tupla: TU9
    // Copre classi: CE1.B, CE2.A, CE3.B, CE4.—, CE5.E (size ≠ N)
    void reorder_sizeMismatch_badRequest() {
        // Arrange
        UUID userId = stubUserWithRole(RolesEnum.ADMIN);
        Listing listing = stubListing(UUID.randomUUID());
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        when(listingMediaRepository.findByListingId(listing.id())).thenReturn(List.of(
                new ListingMedia(p1, listing.id(), UUID.randomUUID(), 1, OffsetDateTime.now(), null),
                new ListingMedia(p2, listing.id(), UUID.randomUUID(), 2, OffsetDateTime.now(), null)
        ));
        // Act + Assert
        List<UUID> ordered = List.of(p1);
        UUID listingId = listing.id();
        assertThrows(BadRequestException.class, () -> service.reorderListingPhotos(userId, listingId, ordered));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Input non valido: contiene ID null -> BadRequest")
    // Tupla: TU7
    // Copre classi: CE1.B, CE2.A, CE3.B, CE4.—, CE5.C (contiene NULL)
    void reorder_containsNullId_badRequest() {
        // Arrange
        UUID userId = stubUserWithRole(RolesEnum.ADMIN);
        Listing listing = stubListing(UUID.randomUUID());
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        when(listingMediaRepository.findByListingId(listing.id())).thenReturn(List.of(
                new ListingMedia(p1, listing.id(), UUID.randomUUID(), 1, OffsetDateTime.now(), null),
                new ListingMedia(p2, listing.id(), UUID.randomUUID(), 2, OffsetDateTime.now(), null)
        ));
        // Act + Assert
        List<UUID> ordered = Arrays.asList(p1, null);
        UUID listingId = listing.id();
        assertThrows(BadRequestException.class, () -> service.reorderListingPhotos(userId, listingId, ordered));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Input non valido: ID duplicati -> BadRequest")
    // Tupla: TU8
    // Copre classi: CE1.B, CE2.A, CE3.B, CE4.—, CE5.D (duplicati a parità di size)
    void reorder_duplicates_badRequest() {
        // Arrange
        UUID userId = stubUserWithRole(RolesEnum.ADMIN);
        Listing listing = stubListing(UUID.randomUUID());
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        when(listingMediaRepository.findByListingId(listing.id())).thenReturn(List.of(
                new ListingMedia(p1, listing.id(), UUID.randomUUID(), 1, OffsetDateTime.now(), null),
                new ListingMedia(p2, listing.id(), UUID.randomUUID(), 2, OffsetDateTime.now(), null)
        ));
        // Act + Assert
        List<UUID> ordered = List.of(p1, p1);
        UUID listingId = listing.id();
        assertThrows(BadRequestException.class, () -> service.reorderListingPhotos(userId, listingId, ordered));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Input non valido: insieme ID non coincide -> BadRequest")
    // Tupla: TU10
    // Copre classi: CE1.B, CE2.A, CE3.B, CE4.—, CE5.G (id mancanti/sconosciuti)
    void reorder_idSetMismatch_badRequest() {
        // Arrange
        UUID userId = stubUserWithRole(RolesEnum.ADMIN);
        Listing listing = stubListing(UUID.randomUUID());
        UUID p1 = UUID.randomUUID();
        when(listingMediaRepository.findByListingId(listing.id())).thenReturn(List.of(
                new ListingMedia(p1, listing.id(), UUID.randomUUID(), 1, OffsetDateTime.now(), null)
        ));
        // Act + Assert
        UUID unknown = UUID.randomUUID();
        List<UUID> ordered = List.of(unknown);
        UUID listingId = listing.id();
        assertThrows(BadRequestException.class, () -> service.reorderListingPhotos(userId, listingId, ordered));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Utente non privilegiato senza profilo agente -> Forbidden")
    // Tupla: TU4
    // Copre classi: CE1.B, CE2.C (Altro), CE3.—, CE4.—, CE5.DC
    void reorder_nonPrivilegedWithoutAgent_forbidden() {
        // Arrange
        UUID userId = stubUserWithRole(RolesEnum.USER); // not privileged
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.empty());
        UUID anyListingId = UUID.randomUUID();
        // Act + Assert
        UUID photoId = UUID.randomUUID();
        List<UUID> ordered = List.of(photoId);
        assertThrows(ForbiddenException.class, () -> service.reorderListingPhotos(userId, anyListingId, ordered));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Listing mancante -> BadRequest")
    // Tupla: TU12
    // Copre classi: CE1.B, CE2.A, CE3.C (listing inesistente), CE4.—, CE5.DC
    void reorder_listingNotFound_badRequest() {
        // Arrange
        UUID userId = stubUserWithRole(RolesEnum.ADMIN);
        UUID missingListingId = UUID.randomUUID();
        when(listingRepository.findById(missingListingId)).thenReturn(Optional.empty());
        // Act + Assert
        UUID photoId = UUID.randomUUID();
        List<UUID> ordered = List.of(photoId);
        assertThrows(BadRequestException.class, () -> service.reorderListingPhotos(userId, missingListingId, ordered));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("listingId NULL -> BadRequest")
    // Tupla: TU11
    // Copre classi: CE1.B, CE2.A, CE3.A (listingId NULL), CE4.—, CE5.(non vuota)
    void reorder_listingIdNull_badRequest() {
        // Arrange
        UUID userId = stubUserWithRole(RolesEnum.ADMIN);
        when(listingRepository.findById(isNull())).thenReturn(Optional.empty());
        // Act + Assert
        UUID photoId = UUID.randomUUID();
        List<UUID> ordered = List.of(photoId);
        assertThrows(BadRequestException.class, () -> service.reorderListingPhotos(userId, null, ordered));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("userId NULL -> Forbidden")
    // Tupla: TU13
    // Copre classi: CE1.A (userId NULL), CE2.—, CE3.B, CE4.—, CE5.(non vuota)
    void reorder_userIdNull_forbidden() {
        // Arrange
        when(agentRepository.findByUserId(isNull())).thenReturn(Optional.empty());
        // Act + Assert
        UUID listingId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        List<UUID> ordered = List.of(photoId);
        assertThrows(ForbiddenException.class, () -> service.reorderListingPhotos(null, listingId, ordered));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("userId inesistente -> Forbidden")
    // Tupla: TU14
    // Copre classi: CE1.C (utente inesistente), CE2.—, CE3.B, CE4.—, CE5.(non vuota)
    void reorder_userIdNotFound_forbidden() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.empty());
        // Act + Assert
        UUID listingId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        List<UUID> ordered = List.of(photoId);
        assertThrows(ForbiddenException.class, () -> service.reorderListingPhotos(userId, listingId, ordered));
    }
}
