package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.ListingPhoto;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.application.media.listing.ListingMediaService;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ListingPhotosApiDelegateImpl implements ListingPhotosApiDelegate {
    private static final Logger log = LoggerFactory.getLogger(ListingPhotosApiDelegateImpl.class);

    private final ListingMediaService listingMediaService;

    public ListingPhotosApiDelegateImpl(ListingMediaService listingMediaService) {
        this.listingMediaService = listingMediaService;
    }


    @Override
    public ResponseEntity<List<ListingPhoto>> listingsIdPhotosPost(
            UUID id,
            @NotNull List<MultipartFile> files,
            @NotNull List<Integer> positions) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Tentativo upload media senza token");
            throw UnauthorizedException.bearerTokenMissing();
        }
        var userId = UUID.fromString(jwtAuth.getToken().getSubject());
        var countPositions = positions.stream().mapToInt(p -> 1).sum();
        var countFiles = files.stream().mapToInt(p -> 1).sum();
        log.info("Tentativo upload foto listing {} da user {}: files={}, positions={}", id, userId, countFiles, countPositions);

        if (countFiles != countPositions) {
            log.warn("Conteggio files ({}) diverso da positions ({}) per listing {} (user {})", countFiles, countPositions, id, userId);
            throw BadRequestException.forField("positions", "I campi 'files' e 'positions' devono contenere lo stesso numero di elementi.");
        }

        try {
            var photos = listingMediaService.uploadListingPhotos(userId, id, files, positions);
            return new ResponseEntity<>(photos, HttpStatus.CREATED);
        } catch (NoSuchElementException exception) {
            log.warn("Listing {} non trovato durante upload foto da user {}", id, userId);
            throw BadRequestException.forField("id", "nessun listing trovato per id '" + id + "'.");
        }
    }
}
