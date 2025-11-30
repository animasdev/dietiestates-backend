package it.dieti.dietiestatesbackend.application.media.listing;

import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingRepository;
import it.dieti.dietiestatesbackend.domain.media.listing.ListingMedia;
import it.dieti.dietiestatesbackend.domain.media.listing.ListingMediaRepository;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetRepository;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.DisplayName("White-box: removeListingPhoto (branch coverage)")
class RemoveListingPhotoWhiteBoxTests {

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

    private Listing listing(UUID ownerAgentId) {
        return new Listing(
                UUID.randomUUID(),
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
    }

    // P1: E1 -> E3 -> E12 -> E13 (privilegiato, listing esiste, foto esiste e appartiene)
    @Test
    @org.junit.jupiter.api.DisplayName("P1: admin success (E1,E3,E12,E13)")
    void p1_admin_success_deletesJoinAndAsset() {
        UUID userId = stubUserWithRole(RolesEnum.ADMIN);
        UUID listingId = UUID.randomUUID();
        UUID listingPhotoId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        var lst = listing(UUID.randomUUID());
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(lst));
        var lm = new ListingMedia(listingPhotoId, listingId, mediaId, 1, OffsetDateTime.now(), null);
        when(listingMediaRepository.findById(listingPhotoId)).thenReturn(Optional.of(lm));

        // Grafo: E1 (ADMIN) -> E3 (listing esiste) -> E12 (photo esiste) -> E13 (match) -> delete
        assertDoesNotThrow(() -> service.removeListingPhoto(userId, listingId, listingPhotoId));

        // Verify collaborations (white-box)
        verify(agentRepository, never()).findByUserId(any()); // privileged path bypasses agent
        verify(listingMediaRepository).delete(listingPhotoId);
        verify(mediaAssetService).deleteAsset(mediaId);
    }

    // P8: E4 -> E6 -> E8 -> E10 -> E12 -> E13 (non-privilegiato, owner, tutto ok)
    @Test
    @org.junit.jupiter.api.DisplayName("P8: user owner success (E4,E6,E8,E10,E12,E13)")
    void p8_userOwner_success() {
        UUID userId = stubUserWithRole(RolesEnum.USER);
        UUID ownerAgentId = UUID.randomUUID();
        Agent agent = new Agent(ownerAgentId, userId, UUID.randomUUID(), null, null, null, null);
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));

        UUID listingId = UUID.randomUUID();
        var lst = listing(ownerAgentId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(lst));

        UUID listingPhotoId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        var lm = new ListingMedia(listingPhotoId, listingId, mediaId, 1, OffsetDateTime.now(), null);
        when(listingMediaRepository.findById(listingPhotoId)).thenReturn(Optional.of(lm));

        // Grafo: E4 (non privilegio) -> E6 (AGENT) -> E8 (listing esiste) -> E10 (owner)
        //        -> E12 (photo esiste) -> E13 (match) -> delete
        assertDoesNotThrow(() -> service.removeListingPhoto(userId, listingId, listingPhotoId));
        verify(listingMediaRepository).delete(listingPhotoId);
        verify(mediaAssetService).deleteAsset(mediaId);
    }

    // P5: E4 -> E6 -> E8 -> E9 (non-privilegiato, non-owner -> Forbidden)
    @Test
    @org.junit.jupiter.api.DisplayName("P5: user non-owner forbidden (E4,E6,E8,E9)")
    void p5_userNotOwner_forbidden() {
        UUID userId = stubUserWithRole(RolesEnum.USER);
        Agent agent = new Agent(UUID.randomUUID(), userId, UUID.randomUUID(), null, null, null, null);
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));

        var lst = listing(UUID.randomUUID()); // different owner
        when(listingRepository.findById(lst.id())).thenReturn(Optional.of(lst));

        UUID listingPhotoId = UUID.randomUUID();
        UUID listingId = lst.id();
        // Grafo: E4 (non privilegio) -> E6 (AGENT) -> E8 (listing esiste) -> E9 (non owner)
        assertThrows(ForbiddenException.class, () -> service.removeListingPhoto(userId, listingId, listingPhotoId));
        verify(listingMediaRepository, never()).delete(any());
        verify(mediaAssetService, never()).deleteAsset(any());
    }

    // P7: E1 -> E3 -> E12 -> E14 (privilegiato, photo mismatch -> BadRequest)
    @Test
    @org.junit.jupiter.api.DisplayName("P7: admin photo mismatch bad request (E1,E3,E12,E14)")
    void p7_photoDoesNotBelongToListing_badRequest() {
        UUID userId = stubUserWithRole(RolesEnum.ADMIN); // privileged to skip ownership
        UUID listingId = UUID.randomUUID();
        var lst = listing(UUID.randomUUID());
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(lst));

        UUID listingPhotoId = UUID.randomUUID();
        UUID otherListingId = UUID.randomUUID();
        var lm = new ListingMedia(listingPhotoId, otherListingId, UUID.randomUUID(), 1, OffsetDateTime.now(), null);
        when(listingMediaRepository.findById(listingPhotoId)).thenReturn(Optional.of(lm));

        // Grafo: E1 (ADMIN) -> E3 (listing esiste) -> E12 (photo esiste) -> E14 (mismatch)
        assertThrows(BadRequestException.class, () -> service.removeListingPhoto(userId, listingId, listingPhotoId));
        verify(listingMediaRepository, never()).delete(any());
        verify(mediaAssetService, never()).deleteAsset(any());
    }

    // P2: E1 -> E2 (privilegiato, listing mancante -> NoSuchElement)
    @Test
    @org.junit.jupiter.api.DisplayName("P2: admin listing not found (E1,E2)")
    void p2_listingNotFound_noSuchElement() {
        UUID userId = stubUserWithRole(RolesEnum.ADMIN);
        UUID listingId = UUID.randomUUID();
        UUID listingPhotoId = UUID.randomUUID();
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        // Grafo: E1 (ADMIN) -> E2 (listing non esiste)
        assertThrows(NoSuchElementException.class, () -> service.removeListingPhoto(userId, listingId, listingPhotoId));
        verify(listingMediaRepository, never()).delete(any());
        verify(mediaAssetService, never()).deleteAsset(any());
    }

    // P3: E4 -> E5 (non-privilegiato, non-AGENT -> Forbidden)
    @Test
    @org.junit.jupiter.api.DisplayName("P3: user without agent forbidden (E4,E5)")
    void p3_userWithoutAgent_forbidden() {
        UUID userId = stubUserWithRole(RolesEnum.USER);
        UUID listingId = UUID.randomUUID();
        var lst = listing(UUID.randomUUID());
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(lst));
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.empty());

        UUID listingPhotoId = UUID.randomUUID();
        // Grafo: E4 (non privilegio) -> E5 (non AGENT)
        assertThrows(ForbiddenException.class, () -> service.removeListingPhoto(userId, listingId, listingPhotoId));
        verify(listingMediaRepository, never()).delete(any());
        verify(mediaAssetService, never()).deleteAsset(any());
    }

    // P4: E4 -> E6 -> E7 (non-privilegiato, AGENT presente, listing mancante)
    @Test
    @org.junit.jupiter.api.DisplayName("P4: user agent listing not found (E4,E6,E7)")
    void p4_userAgent_listingNotFound_noSuchElement() {
        UUID userId = stubUserWithRole(RolesEnum.USER);
        Agent agent = new Agent(UUID.randomUUID(), userId, UUID.randomUUID(), null, null, null, null);
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));

        UUID listingId = UUID.randomUUID();
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        UUID listingPhotoId = UUID.randomUUID();
        // Grafo: E4 (non privilegio) -> E6 (AGENT) -> E7 (listing non esiste)
        assertThrows(NoSuchElementException.class, () -> service.removeListingPhoto(userId, listingId, listingPhotoId));
        verify(listingMediaRepository, never()).delete(any());
        verify(mediaAssetService, never()).deleteAsset(any());
    }

    // P6: E4 -> E6 -> E8 -> E10 -> E11 (non-privilegiato owner, photo mancante)
    @Test
    @org.junit.jupiter.api.DisplayName("P6: user owner photo not found (E4,E6,E8,E10,E11)")
    void p6_userOwner_photoNotFound_noSuchElement() {
        UUID userId = stubUserWithRole(RolesEnum.USER);
        UUID ownerAgentId = UUID.randomUUID();
        Agent agent = new Agent(ownerAgentId, userId, UUID.randomUUID(), null, null, null, null);
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));

        UUID listingId = UUID.randomUUID();
        var lst = listing(ownerAgentId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(lst));

        UUID listingPhotoId = UUID.randomUUID();
        when(listingMediaRepository.findById(listingPhotoId)).thenReturn(Optional.empty());

        // Grafo: E4 (non privilegio) -> E6 (AGENT) -> E8 (listing esiste) -> E10 (owner) -> E11 (photo non esiste)
        assertThrows(NoSuchElementException.class, () -> service.removeListingPhoto(userId, listingId, listingPhotoId));
        verify(listingMediaRepository, never()).delete(any());
        verify(mediaAssetService, never()).deleteAsset(any());
    }

    // P7 already covered above; P8 covered above.
}
