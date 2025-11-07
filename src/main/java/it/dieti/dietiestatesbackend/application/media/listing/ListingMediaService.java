package it.dieti.dietiestatesbackend.application.media.listing;

import it.dieti.dietiestatesbackend.api.model.ListingPhoto;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.application.exception.listing.AgentProfileRequiredException;
import it.dieti.dietiestatesbackend.application.media.MediaAssetService;
import it.dieti.dietiestatesbackend.domain.agent.AgentRepository;
import it.dieti.dietiestatesbackend.domain.listing.ListingRepository;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetRepository;
import it.dieti.dietiestatesbackend.domain.media.listing.ListingMedia;
import it.dieti.dietiestatesbackend.domain.media.listing.ListingMediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ListingMediaService {

    private static final Logger log = LoggerFactory.getLogger(ListingMediaService.class);
    private static final String POSITIONS_FIELD = "positions";
    private static final String ID_FIELD = "id";
    private final ListingRepository listingRepository;
    private final MediaAssetService mediaAssetService;
    private final ListingMediaRepository listingMediaRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final AgentRepository agentRepository;

    public ListingMediaService(ListingRepository listingRepository,
                               MediaAssetService mediaAssetService,
                               ListingMediaRepository listingMediaRepository,
                               MediaAssetRepository mediaAssetRepository,
                               AgentRepository agentRepository) {
        this.listingRepository = listingRepository;
        this.mediaAssetService = mediaAssetService;
        this.listingMediaRepository = listingMediaRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.agentRepository = agentRepository;
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
                .orElseThrow(() -> new NoSuchElementException("listing"));

        if (!agent.id().equals(listing.ownerAgentId())) {
            throw ForbiddenException.actionRequiresRole("proprietario");
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
                .orElseThrow(() -> new NoSuchElementException("listing"));

        if (!agent.id().equals(listing.ownerAgentId())) {
            throw ForbiddenException.actionRequiresRole("proprietario");
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
}
