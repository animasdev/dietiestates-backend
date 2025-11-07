package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.ListingPhoto;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.application.media.listing.ListingMediaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ListingPhotosApiDelegateImpl implements ListingPhotosApiDelegate {
    public static final String NO_LISTING_ID_MESSAGE = "nessun listing trovato per id '";
    private static final Logger log = LoggerFactory.getLogger(ListingPhotosApiDelegateImpl.class);
    public static final String LISTING = "listing";

    private final ListingMediaService listingMediaService;

    public ListingPhotosApiDelegateImpl(ListingMediaService listingMediaService) {
        this.listingMediaService = listingMediaService;
    }


    @Override
    public ResponseEntity<List<ListingPhoto>> listingsIdPhotosPost(
            UUID id,
            List<@Valid ListingPhoto> listingPhotos) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Tentativo upload media senza token");
            throw UnauthorizedException.bearerTokenMissing();
        }
        var userId = UUID.fromString(jwtAuth.getToken().getSubject());

        try {
            var photos = listingMediaService.attachListingPhotos(userId,id,listingPhotos);
            return new ResponseEntity<>(photos, HttpStatus.CREATED);
        } catch (NoSuchElementException exception) {
            log.warn(exception.getMessage());
            if (exception.getMessage().equals(LISTING))
                throw BadRequestException.forField("id", NO_LISTING_ID_MESSAGE + id + "'.");
            else
                throw BadRequestException.of(exception.getMessage());
        }
    }

    @Override
    public ResponseEntity<Void> listingsIdPhotosPhotoIdDelete(UUID id, UUID photoId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Tentativo delete media senza token");
            throw UnauthorizedException.bearerTokenMissing();
        }
        var userId = UUID.fromString(jwtAuth.getToken().getSubject());

        try {
            listingMediaService.removeListingPhoto(userId, id, photoId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NoSuchElementException exception) {
            log.warn(exception.getMessage());
            if (LISTING.equals(exception.getMessage())) {
                throw BadRequestException.forField("id", NO_LISTING_ID_MESSAGE + id + "'.");
            } else if ("photo".equals(exception.getMessage())) {
                throw BadRequestException.forField("photoId", "nessuna foto trovata per id '" + photoId + "'.");
            } else {
                throw BadRequestException.of(exception.getMessage());
            }
        }
    }

    @Override
    public ResponseEntity<List<ListingPhoto>> listingsIdPhotosOrderPut(UUID id, List<UUID> body) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Tentativo reorder media senza token");
            throw UnauthorizedException.bearerTokenMissing();
        }
        var userId = UUID.fromString(jwtAuth.getToken().getSubject());

        try {
            var views = listingMediaService.reorderListingPhotos(userId, id, body);
            var result = views.stream()
                    .map(v -> new ListingPhoto().id(v.id()).url(URI.create(v.publicUrl())).position(v.position()))
                    .toList();
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (NoSuchElementException exception) {
            log.warn(exception.getMessage());
            if (LISTING.equals(exception.getMessage())) {
                throw BadRequestException.forField("id", NO_LISTING_ID_MESSAGE + id + "'.");
            } else {
                throw BadRequestException.of(exception.getMessage());
            }
        }
    }
}
