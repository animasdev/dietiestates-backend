package it.dieti.dietiestatesbackend.application.media.listing;

import it.dieti.dietiestatesbackend.api.model.ListingPhoto;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.application.exception.listing.AgentProfileRequiredException;
import it.dieti.dietiestatesbackend.application.media.MediaAssetService;
import it.dieti.dietiestatesbackend.domain.user.agent.AgentRepository;
import it.dieti.dietiestatesbackend.domain.listing.ListingRepository;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetRepository;
import it.dieti.dietiestatesbackend.domain.media.listing.ListingMedia;
import it.dieti.dietiestatesbackend.domain.media.listing.ListingMediaRepository;
import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RoleRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RolesEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ListingMediaService {

    private static final Logger log = LoggerFactory.getLogger(ListingMediaService.class);
    private static final String POSITIONS_FIELD = "positions";
    private static final String ID_FIELD = "id";
    public static final String LISTING = "listing";
    public static final String PROPRIETARIO = "proprietario";
    public static final String PHOTO_IDS = "photoIds";
    private final ListingRepository listingRepository;
    private final MediaAssetService mediaAssetService;
    private final ListingMediaRepository listingMediaRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public ListingMediaService(ListingRepository listingRepository,
                               MediaAssetService mediaAssetService,
                               ListingMediaRepository listingMediaRepository,
                               MediaAssetRepository mediaAssetRepository,
                               AgentRepository agentRepository,
                               UserRepository userRepository,
                               RoleRepository roleRepository) {
        this.listingRepository = listingRepository;
        this.mediaAssetService = mediaAssetService;
        this.listingMediaRepository = listingMediaRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.agentRepository = agentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * @deprecated
     */
    @Transactional
    @Deprecated(since = "2025-11-04")
    public List<ListingPhoto> uploadListingPhotos(UUID userId,
                                                  UUID listingId,
                                                  List<MultipartFile> files,
                                                  List<Integer> positions) {
        var agent = agentRepository.findByUserId(userId)
                .orElseThrow(AgentProfileRequiredException::new);
        var listing = listingRepository.findById(listingId)
                .orElseThrow(() -> BadRequestException.forField(LISTING, "Annuncio inesistente."));

        if (!agent.id().equals(listing.ownerAgentId())) {
            throw ForbiddenException.actionRequiresRole(PROPRIETARIO);
        }
        // Validate positions before any upload occurs
        if (positions == null || files == null || positions.size() != files.size()) {
            throw BadRequestException.forField(POSITIONS_FIELD, "Numero di posizioni non coerente con i file.");
        }

        Set<Integer> seen = new HashSet<>();
        for (Integer pos : positions) {
            if (pos == null || pos < 1) {
                throw BadRequestException.forField(POSITIONS_FIELD, "Le posizioni devono essere valori interi positivi univoci.");
            }
            if (!seen.add(pos)) {
                throw BadRequestException.forField(POSITIONS_FIELD, "Le posizioni nella richiesta devono essere univoche.");
            }
        }
        var existingPositions = listingMediaRepository.findByListingId(listingId).stream()
                .map(ListingMedia::sortOrder)
                .collect(Collectors.toSet());
        for (Integer pos : seen) {
            if (existingPositions.contains(pos)) {
                throw BadRequestException.forField(POSITIONS_FIELD, "Posizione già assegnata a una foto esistente.");
            }
        }

        List<ListingPhoto> photos = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            var file = files.get(index);
            var media = mediaAssetService.upload(userId, "LISTING_PHOTO", file);
            var position = positions.get(index);
            var listingMedia = new ListingMedia(
                    null,
                    listing.id(),
                    media.id(),
                    position,
                    null,
                    null
            );
            var saved = listingMediaRepository.save(listingMedia);
            photos.add(new ListingPhoto()
                    .id(saved.id())
                    .url(URI.create(media.publicUrl()))
                    .position(position));
        }
        return photos;
    }

    @Transactional
    public List<ListingPhoto> attachListingPhotos(UUID userId, UUID listingId, List<ListingPhoto> photos) {
        var agent = agentRepository.findByUserId(userId)
                .orElseThrow(AgentProfileRequiredException::new);
        var listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException(LISTING));

        if (!agent.id().equals(listing.ownerAgentId())) {
            throw ForbiddenException.actionRequiresRole(PROPRIETARIO);
        }

        if (photos == null || photos.isEmpty()) {
            log.warn("Tentativo di associazione foto a listing {} da user {} senza foto", listingId, userId);
            throw BadRequestException.forField("photos", "array 'photos' è vuoto");
        }

        validatePhotoList(listingId,photos,userId);
        List<ListingPhoto> addedPhotos = new ArrayList<>();
        for (ListingPhoto photo : photos) {
            var media = mediaAssetService.getListingPhoto(photo.getId());
            var listingMedia = new ListingMedia(
                    null,
                    listing.id(),
                    media.id(),
                    photo.getPosition(),
                    null,
                    null
            );
            var saved = listingMediaRepository.save(listingMedia);
            addedPhotos.add(new ListingPhoto()
                    .id(saved.id())
                    .url(URI.create(media.publicUrl()))
                    .position(photo.getPosition()));
        }
        return addedPhotos;
    }

    private void validatePhotoList(UUID listingId, List<ListingPhoto> photos, UUID userId) {
        if (photos == null || photos.isEmpty()) {
            return;
        }

        Set<Integer> seenPositions = new HashSet<>();
        for (ListingPhoto photo : photos) {
            var position = photo.getPosition();
            if (position == null || position < 1) {
                log.warn("Posizione non valida {} per listing {} (user {})", position, listingId, userId);
                throw BadRequestException.forField(POSITIONS_FIELD, "Le posizioni devono essere valori interi positivi univoci.");
            }
            if (!seenPositions.add(position)) {
                log.warn("Posizione duplicata {} nella stessa richiesta per listing {} (user {})", position, listingId, userId);
                throw BadRequestException.forField(POSITIONS_FIELD, "Le posizioni nella richiesta devono essere univoche.");
            }
            var duplicated = listingMediaRepository.findByMediaId(photo.getId());
            if (!duplicated.isEmpty()){
                log.warn("media '{}' associato a listing '{}' ", photo.getId(), duplicated.getFirst().listingId());
                throw BadRequestException.forField(ID_FIELD,"media '"+photo.getId()+" già associato ad un listing.");
            }
        }

        var existingPositions = listingMediaRepository.findByListingId(listingId).stream()
                .map(ListingMedia::sortOrder)
                .collect(Collectors.toSet());
        for (Integer position : seenPositions) {
            if (existingPositions.contains(position)) {
                log.warn("Posizione {} già occupata per listing {} (user {})", position, listingId, userId);
                throw BadRequestException.forField(POSITIONS_FIELD, "Posizione già assegnata a una foto esistente.");
            }
        }
    }

    public List<ListingPhotoView> getListingPhotos(UUID listingId) {
        return listingMediaRepository.findByListingId(listingId).stream()
                .map(media -> mediaAssetRepository.findById(media.mediaId())
                        .map(asset -> new ListingPhotoView(media.id(), asset.publicUrl(), media.sortOrder()))
                        .orElseGet(() -> {
                            log.warn("Media {} non trovato durante fetch foto listing {}", media.mediaId(), listingId);
                            return null;
                        }))
                .filter(Objects::nonNull)
                .toList();
    }

    public record ListingPhotoView(UUID id, String publicUrl, Integer position) {}

    public void removeListingPhoto(UUID userId, UUID listingId, UUID listingPhotoId) {
        var userRole = resolveUserRole(userId);
        boolean isPrivileged = userRole == RolesEnum.ADMIN || userRole == RolesEnum.SUPERADMIN;

        var agent = isPrivileged ? null : agentRepository.findByUserId(userId)
                .orElseThrow(() -> ForbiddenException.actionRequiresRoles(null));
        var listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException(LISTING));

        if (!isPrivileged) {
            if (!agent.id().equals(listing.ownerAgentId())) {
                throw ForbiddenException.actionRequiresRole(PROPRIETARIO);
            }
        } else {
            log.info("Privileged user {} with role {} removed photo {} from listing {}", userId, userRole, listingPhotoId, listingId);
        }

        var listingMedia = listingMediaRepository.findById(listingPhotoId)
                .orElseThrow(() -> new NoSuchElementException("photo"));

        if (!listingMedia.listingId().equals(listingId)) {
            throw BadRequestException.forField("photoId", "la foto non appartiene al listing indicato");
        }

        // Keep reference before deleting the join
        var mediaId = listingMedia.mediaId();
        listingMediaRepository.delete(listingPhotoId);

        // Also delete the underlying media asset (DB + storage) in a best-effort manner
        mediaAssetService.deleteAsset(mediaId);
    }

    @Transactional
    public List<ListingPhotoView> reorderListingPhotos(UUID userId, UUID listingId, List<UUID> orderedPhotoIds) {
        var userRole = resolveUserRole(userId);
        boolean isPrivileged = userRole == RolesEnum.ADMIN || userRole == RolesEnum.SUPERADMIN;

        // Per utenti non privilegiati, verifichiamo prima il profilo agente (autorizzazione), poi l'esistenza del listing
        var agent = isPrivileged ? null : agentRepository.findByUserId(userId)
                .orElseThrow(() -> ForbiddenException.of("Permesso negato: profilo agente richiesto."));

        var listing = listingRepository.findById(listingId)
                .orElseThrow(() -> BadRequestException.forField(LISTING, "Annuncio inesistente."));

        if (!isPrivileged && !agent.id().equals(listing.ownerAgentId())) {
                throw ForbiddenException.actionRequiresRole(PROPRIETARIO);

        }

        if (orderedPhotoIds == null || orderedPhotoIds.isEmpty()) {
            throw BadRequestException.forField(PHOTO_IDS, "Elenco foto obbligatorio per il riordino.");
        }

        var existing = listingMediaRepository.findByListingId(listingId);
        if (existing.isEmpty()) {
            // nothing to reorder — treat as bad request to surface mismatch
            throw BadRequestException.forField(PHOTO_IDS, "Nessuna foto esistente per l'annuncio.");
        }

        // Validate exact match and uniqueness
        if (orderedPhotoIds.size() != existing.size()) {
            throw BadRequestException.forField(PHOTO_IDS, "L'elenco deve contenere tutte e sole le foto correnti.");
        }
        Set<UUID> seen = new HashSet<>();
        for (UUID pid : orderedPhotoIds) {
            if (pid == null) {
                throw BadRequestException.forField(PHOTO_IDS, "ID foto non valido.");
            }
            if (!seen.add(pid)) {
                throw BadRequestException.forField(PHOTO_IDS, "ID foto duplicati non consentiti.");
            }
        }
        var existingIds = existing.stream().map(ListingMedia::id).collect(Collectors.toSet());
        if (!existingIds.equals(seen)) {
            throw BadRequestException.forField(PHOTO_IDS, "Gli ID non corrispondono alle foto correnti del listing.");
        }

        // Phase 1: bulk-bump all current rows out of the target range to avoid unique constraint conflicts
        // Use a large offset that won't collide with existing orders
        final int OFFSET = 1000;
        listingMediaRepository.bumpSortOrders(listingId, OFFSET);

        // Phase 2: apply new contiguous order 1..N
        int position = 1;
        for (UUID pid : orderedPhotoIds) {
            var lm = existing.stream().filter(e -> e.id().equals(pid)).findFirst().orElseThrow();
            var updated = new ListingMedia(
                    lm.id(),
                    lm.listingId(),
                    lm.mediaId(),
                    position++,
                    lm.createdAt(),
                    null
            );
            listingMediaRepository.save(updated);
        }

        return getListingPhotos(listingId);
    }

    private RolesEnum resolveUserRole(UUID userId) {
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }
        var roleOpt = roleRepository.findById(userOpt.get().roleId());
        if (roleOpt.isEmpty()) {
            return null;
        }
        try {
            return RolesEnum.valueOf(roleOpt.get().code());
        } catch (IllegalArgumentException ex) {
            log.warn("Unsupported role code {} for user {}", roleOpt.get().code(), userId, ex);
            return null;
        }
    }
}
