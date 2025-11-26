package it.dieti.dietiestatesbackend.api;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import it.dieti.dietiestatesbackend.api.model.DeleteRequest;
import it.dieti.dietiestatesbackend.api.model.Listing;
import it.dieti.dietiestatesbackend.api.model.ListingCreate;
import it.dieti.dietiestatesbackend.api.model.ListingGeo;
import it.dieti.dietiestatesbackend.api.model.ListingPhoto;
import it.dieti.dietiestatesbackend.api.model.Page;
import it.dieti.dietiestatesbackend.api.model.ListingUpdate;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.InternalServerErrorException;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.application.exception.listing.ListingStatusUnavailableException;
import it.dieti.dietiestatesbackend.application.exception.listing.ListingTypeNotSupportedException;
import it.dieti.dietiestatesbackend.application.feature.FeatureService;
import it.dieti.dietiestatesbackend.application.listing.ListingCreationService;
import it.dieti.dietiestatesbackend.application.listing.ListingSearchService;
import it.dieti.dietiestatesbackend.application.media.listing.ListingMediaService;
import it.dieti.dietiestatesbackend.domain.feature.Feature;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ListingsApiDelegateImpl implements ListingsApiDelegate {

    private static final String LISTING = "listing";
    private final ListingCreationService listingCreationService;
    private final ListingMediaService listingMediaService;
    private final FeatureService featureService;
    private final ListingSearchService listingSearchService;
    private static final String NO_LISTING_BY_ID = "nessun listing trovato per id";
    private static final Logger log = LoggerFactory.getLogger(ListingsApiDelegateImpl.class);

    public ListingsApiDelegateImpl(ListingCreationService listingCreationService,
                                   ListingMediaService listingMediaService,
                                   FeatureService featureService,
                                   ListingSearchService listingSearchService) {
        this.listingCreationService = listingCreationService;
        this.listingMediaService = listingMediaService;
        this.featureService = featureService;
        this.listingSearchService = listingSearchService;
    }


    @Override
    public ResponseEntity<Page> listingsGet(
            String type,
            String city,
            Integer minPrice,
            Integer maxPrice,
            Integer minRooms,
            Integer maxRooms,
            Float minSqm,
            Float maxSqm,
            Boolean furnished,
            Boolean petsAllowed,
            List<String> energyClasses,
            List<String> postalCodes,
            List<String> features,
            String status,
            Float lat,
            Float lng,
            Integer radiusMeters,
            Boolean hasPhotos,
            UUID agencyId,
            UUID ownerAgentId,
            Integer page,
            Integer size,
            String sort
    ) {
        boolean enforcePublishedOnly = !(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken);
        String normalizedStatus = status != null ? status.trim().toUpperCase(Locale.ROOT) : null;
        Double latitude = lat != null ? lat.doubleValue() : null;
        Double longitude = lng != null ? lng.doubleValue() : null;

        var query = new ListingSearchService.SearchQuery(
                type,
                city,
                minPrice,
                maxPrice,
                minRooms,
                maxRooms,
                minSqm != null ? java.math.BigDecimal.valueOf(minSqm) : null,
                maxSqm != null ? java.math.BigDecimal.valueOf(maxSqm) : null,
                energyClasses,
                features,
                postalCodes,
                normalizedStatus,
                latitude,
                longitude,
                radiusMeters,
                hasPhotos,
                furnished,
                petsAllowed,
                agencyId,
                ownerAgentId,
                page,
                size,
                sort,
                enforcePublishedOnly
        );

        try {
            var result = listingSearchService.search(query);
            var items = result.items().stream()
                    .map(item -> toApi(
                            item.listing(),
                            item.listingStatus() != null ? item.listingStatus().code() : null,
                            item.listingType() != null ? item.listingType().code() : null,
                            item.photos().stream().map(this::toApi).toList(),
                            item.features().stream().map(Feature::code).toList()
                    ))
                    .toList();

            Page body = new Page();
            body.setPage(result.page());
            body.setSize(result.size());
            body.setTotal(Math.toIntExact(result.total()));
            body.setItems(items);
            return ResponseEntity.ok(body);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Errore inatteso durante la ricerca annunci", ex);
            throw new InternalServerErrorException("Si è verificato un errore interno. Riprova più tardi.");
        }
    }

    @Override
    public ResponseEntity<java.util.List<ListingPhoto>> listingsIdPhotosPost(
            UUID id,
            java.util.List<@jakarta.validation.Valid ListingPhoto> listingPhotos) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Tentativo upload media senza token");
            throw UnauthorizedException.bearerTokenMissing();
        }
        var userId = UUID.fromString(jwtAuth.getToken().getSubject());

        try {
            var photos = listingMediaService.attachListingPhotos(userId, id, listingPhotos);
            return new ResponseEntity<>(photos, HttpStatus.CREATED);
        } catch (java.util.NoSuchElementException exception) {
            log.warn(exception.getMessage());
            if (exception.getMessage().equals(LISTING))
                throw it.dieti.dietiestatesbackend.application.exception.BadRequestException.forField("id", NO_LISTING_BY_ID + " '" + id + "'.");
            else
                throw it.dieti.dietiestatesbackend.application.exception.BadRequestException.of(exception.getMessage());
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
        } catch (java.util.NoSuchElementException exception) {
            log.warn(exception.getMessage());
            if (LISTING.equals(exception.getMessage())) {
                throw it.dieti.dietiestatesbackend.application.exception.BadRequestException.forField("id", NO_LISTING_BY_ID + " '" + id + "'.");
            } else if ("photo".equals(exception.getMessage())) {
                throw it.dieti.dietiestatesbackend.application.exception.BadRequestException.forField("photoId", "nessuna foto trovata per id '" + photoId + "'.");
            } else {
                throw it.dieti.dietiestatesbackend.application.exception.BadRequestException.of(exception.getMessage());
            }
        }
    }

    @Override
    public ResponseEntity<java.util.List<ListingPhoto>> listingsIdPhotosOrderPut(UUID id, java.util.List<UUID> body) {
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
        } catch (java.util.NoSuchElementException exception) {
            log.warn(exception.getMessage());
            if (LISTING.equals(exception.getMessage())) {
                throw it.dieti.dietiestatesbackend.application.exception.BadRequestException.forField("id", NO_LISTING_BY_ID + " '" + id + "'.");
            } else {
                throw it.dieti.dietiestatesbackend.application.exception.BadRequestException.of(exception.getMessage());
            }
        }
    }

    @Override
    public ResponseEntity<Listing> listingsPost(ListingCreate listingCreate) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Accesso non autorizzato a listingsPost: token mancante");
            throw UnauthorizedException.bearerTokenMissing();
        }

        requireField(listingCreate.getListingType(), "listingType");
        requireField(listingCreate.getGeo(), "geo");
        requireField(listingCreate.getEnergyClass(), "energyClass");

        var geo = listingCreate.getGeo();
        requireField(geo.getLat(), "geo.lat");
        requireField(geo.getLng(), "geo.lng");

        Long priceCents = listingCreate.getPriceCents() != null
                ? listingCreate.getPriceCents().longValue()
                : null;
        Long securityDepositCents = listingCreate.getSecurityDepositCents() != null
                ? listingCreate.getSecurityDepositCents()
                : null;
        Long condoFeeCents = listingCreate.getCondoFeeCents() != null
                ? listingCreate.getCondoFeeCents()
                : null;

        var userId = UUID.fromString(jwtAuth.getToken().getSubject());

        var featureCodes = listingCreate.getFeatures() == null ? List.<String>of() : List.copyOf(listingCreate.getFeatures());
        var command = new ListingCreationService.CreateListingCommand(
                listingCreate.getTitle(),
                listingCreate.getDescription(),
                listingCreate.getListingType().getValue(),
                priceCents,
                listingCreate.getSizeSqm() != null ? BigDecimal.valueOf(listingCreate.getSizeSqm()) : null,
                listingCreate.getRooms(),
                listingCreate.getFloor(),
                listingCreate.getEnergyClass().getValue(),
                listingCreate.getContractDescription(),
                securityDepositCents,
                listingCreate.getFurnished(),
                condoFeeCents,
                listingCreate.getPetsAllowed(),
                listingCreate.getAddress(),
                listingCreate.getCity(),
                listingCreate.getPostalCode(),
                geo.getLat(),
                geo.getLng(),
                listingCreate.getIsPublished(),
                featureCodes
        );

        try {
            var listing = listingCreationService.createListingForUser(userId, command);
            return ResponseEntity.status(HttpStatus.CREATED).body(getFullListing(listing.id(),userId));
        } catch (ListingTypeNotSupportedException ex) {
            var acceptedTypes = Arrays.stream(ListingCreate.ListingTypeEnum.values())
                    .map(ListingCreate.ListingTypeEnum::getValue)
                    .collect(Collectors.joining(", "));
            var message = "listingType non supportato. Tipi ammessi: " + acceptedTypes + ".";
            log.warn("Listing type non supportato ({}) per user {}", ex.requestedType(), userId);
            throw BadRequestException.forField("listingType", message);
        } catch (ListingStatusUnavailableException ex) {
            log.error("Listing status {} non disponibile durante la creazione (user {})", ex.statusCode(), userId, ex);
            throw new InternalServerErrorException("Si è verificato un errore interno. Riprova più tardi.");
        } catch (IllegalStateException ex) {
            log.error("Errore interno durante la creazione listing per user {}", userId, ex);
            throw new InternalServerErrorException("Si è verificato un errore interno. Riprova più tardi.");
        }
    }


    @Override
    public ResponseEntity<Listing> listingsIdGet(
            @Parameter(name = "id", required = true, in = ParameterIn.PATH) UUID id
    ){
        UUID userId = null;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            userId = UUID.fromString(jwtAuth.getToken().getSubject());
        }

        var body = getFullListing(id,userId);
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    @Override
    public ResponseEntity<Listing> listingsIdPatch(
            @Parameter(name = "id", required = true, in = ParameterIn.PATH) UUID id,
            ListingUpdate listingUpdate
    ) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Accesso non autorizzato a listingsIdPatch: token mancante");
            throw UnauthorizedException.bearerTokenMissing();
        }

        var userId = UUID.fromString(jwtAuth.getToken().getSubject());

        Long priceCents = listingUpdate.getPriceCents() != null
                ? listingUpdate.getPriceCents().longValue()
                : null;
        BigDecimal sizeSqm = listingUpdate.getSizeSqm() != null
                ? BigDecimal.valueOf(listingUpdate.getSizeSqm())
                : null;
        var geo = listingUpdate.getGeo();
        Double latitude = geo != null ? Double.valueOf(geo.getLat()) : null;
        Double longitude = geo != null ? Double.valueOf(geo.getLng()) : null;
        var featureCodes = listingUpdate.getFeatures() != null ? List.copyOf(listingUpdate.getFeatures()) : null;
        Long securityDepositCents = listingUpdate.getSecurityDepositCents() != null
                ? listingUpdate.getSecurityDepositCents()
                : null;
        Long condoFeeCents = listingUpdate.getCondoFeeCents() != null
                ? listingUpdate.getCondoFeeCents()
                : null;

        var command = new ListingCreationService.UpdateListingCommand(
                listingUpdate.getTitle(),
                listingUpdate.getDescription(),
                priceCents,
                sizeSqm,
                listingUpdate.getRooms(),
                listingUpdate.getFloor(),
                listingUpdate.getEnergyClass().getValue(),
                listingUpdate.getContractDescription(),
                securityDepositCents,
                listingUpdate.getFurnished(),
                condoFeeCents,
                listingUpdate.getPetsAllowed(),
                listingUpdate.getAddress(),
                listingUpdate.getCity(),
                listingUpdate.getPostalCode(),
                latitude,
                longitude,
                featureCodes
        );

        var updatedListing = listingCreationService.updateListingForUser(userId, id, command);
        return ResponseEntity.status(HttpStatus.OK).body(getFullListing(updatedListing.id(),userId));
    }

    @Override
    public ResponseEntity<Listing> listingsIdDeletePost(
            @Parameter(name = "id", required = true, in = ParameterIn.PATH) UUID id,
            DeleteRequest deleteRequest
    ) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Accesso non autorizzato a listingsIdDeletePost: token mancante");
            throw UnauthorizedException.bearerTokenMissing();
        }

        var userId = UUID.fromString(jwtAuth.getToken().getSubject());
        var listing = listingCreationService.requestDeletion(userId, id,deleteRequest.getReason());
        log.info("Listing {} contrassegnato per cancellazione da user {}", id, userId);
        return ResponseEntity.status(HttpStatus.OK).body(getFullListing(listing.id(),userId));
    }

    @Override
    public ResponseEntity<Listing> listingsIdRestorePost(
            @Parameter(name = "id", required = true, in = ParameterIn.PATH) UUID id
    ) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Accesso non autorizzato a listingsIdRestorePost: token mancante");
            throw UnauthorizedException.bearerTokenMissing();
        }

        var userId = UUID.fromString(jwtAuth.getToken().getSubject());
        var listing = listingCreationService.restoreListing(userId, id);
        log.info("Listing {} ripristinato da user {}", id, userId);
        return ResponseEntity.status(HttpStatus.OK).body(getFullListing(listing.id(),userId));
    }


    @Override
    public ResponseEntity<Listing> listingsIdPublishPost(
            @Parameter(name = "id", required = true, in = ParameterIn.PATH) UUID id
    ) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Accesso non autorizzato a listingsIdPublishPost: token mancante");
            throw UnauthorizedException.bearerTokenMissing();
        }
        var userId = UUID.fromString(jwtAuth.getToken().getSubject());
        var listing = listingCreationService.publishListing(userId, id);
        return ResponseEntity.status(HttpStatus.OK).body(getFullListing(listing.id(), userId));
    }


    @Override
    public ResponseEntity<Listing> listingsIdDraftPost(
            @Parameter(name = "id", required = true, in = ParameterIn.PATH) UUID id,
            it.dieti.dietiestatesbackend.api.model.DraftRequest draftRequest
    ) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Accesso non autorizzato a listingsIdDraftPost: token mancante");
            throw UnauthorizedException.bearerTokenMissing();
        }
        var userId = UUID.fromString(jwtAuth.getToken().getSubject());
        var listing = listingCreationService.moveListingToDraft(userId, id, draftRequest != null ? draftRequest.getReason() : null);
        return ResponseEntity.status(HttpStatus.OK).body(getFullListing(listing.id(), userId));
    }

    private Listing getFullListing(UUID id, UUID userId) {
        var listingDetails = listingCreationService.getListingDetails(id,userId);
        var photos = listingMediaService.getListingPhotos(id);
        var features = featureService.getListingFeatures(id).stream().map(Feature::code).toList();
        return toApi(
                listingDetails.listing(),
                listingDetails.listingStatus().code(),
                listingDetails.listingType().code(),
                photos.stream().map(this::toApi).toList(),
                features
        );
    }

    private void requireField(Object value, String field) {
        if (value == null || (value instanceof String str && str.isBlank())) {
            var message = "Il campo '" + field + "' è obbligatorio.";
            log.warn("Validazione listingCreate fallita: {}", message);
            throw BadRequestException.forField(field, message);
        }
    }

    private ListingPhoto toApi(ListingMediaService.ListingPhotoView photoView){
        return new ListingPhoto()
                .id(photoView.id())
                .url(URI.create(photoView.publicUrl()))
                .position(photoView.position());
    }

    private Listing toApi(it.dieti.dietiestatesbackend.domain.listing.Listing listing,String listingStatus, String typeCode,List<ListingPhoto> photos, List<String> features) {
        Listing body = new Listing();
        body.setId(listing.id());
        body.setAgencyId(listing.agencyId());
        body.setOwnerAgentId(listing.ownerAgentId());
        body.setListingType(Listing.ListingTypeEnum.valueOf(typeCode));
        body.setStatus(Listing.StatusEnum.valueOf(listingStatus));
        body.setTitle(listing.title());
        body.setDescription(listing.description());
        body.setPriceCents(Math.toIntExact(listing.priceCents()));
        body.setCity(listing.city());
        body.setAddress(listing.addressLine());
        body.setPostalCode(listing.postalCode());
        body.setRooms(listing.rooms());
        body.setSizeSqm(listing.sizeSqm() != null ? listing.sizeSqm().floatValue() : null);
        body.setFloor(listing.floor());
        body.setEnergyClass(Listing.EnergyClassEnum.valueOf(listing.energyClass()));
        body.setContractDescription(listing.contractDescription());
        body.setSecurityDepositCents(listing.securityDepositCents());
        body.setFurnished(listing.furnished());
        body.setCondoFeeCents(listing.condoFeeCents());
        body.setPetsAllowed(listing.petsAllowed());
        body.setFeatures(features);
        body.setGeo(toGeo(listing.geo()));
        body.setPhotos(photos);
        body.setCreatedAt(listing.createdAt());
        body.setUpdatedAt(listing.updatedAt());
        return body;
    }

    private ListingGeo toGeo(Point point) {
        if (point == null) {
            return null;
        }
        var geo = new ListingGeo();
        geo.setLat((float) point.getY());
        geo.setLng((float) point.getX());
        return geo;
    }

}
