package it.dieti.dietiestatesbackend.api;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import it.dieti.dietiestatesbackend.api.model.Listing;
import it.dieti.dietiestatesbackend.api.model.ListingCreate;
import it.dieti.dietiestatesbackend.api.model.ListingGeo;
import it.dieti.dietiestatesbackend.api.model.ListingPhoto;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.InternalServerErrorException;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.application.exception.listing.ListingStatusUnavailableException;
import it.dieti.dietiestatesbackend.application.exception.listing.ListingTypeNotSupportedException;
import it.dieti.dietiestatesbackend.application.listing.ListingCreationService;
import it.dieti.dietiestatesbackend.application.media.listing.ListingMediaService;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusesEnum;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ListingsApiDelegateImpl implements ListingsApiDelegate {

    private final ListingCreationService listingCreationService;
    private final ListingMediaService listingMediaService;
    private static final Logger log = LoggerFactory.getLogger(ListingsApiDelegateImpl.class);

    public ListingsApiDelegateImpl(ListingCreationService listingCreationService, ListingMediaService listingMediaService) {
        this.listingCreationService = listingCreationService;
        this.listingMediaService = listingMediaService;
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

        var geo = listingCreate.getGeo();
        requireField(geo.getLat(), "geo.lat");
        requireField(geo.getLng(), "geo.lng");

        Long priceCents = listingCreate.getPriceCents() != null
                ? listingCreate.getPriceCents().longValue()
                : null;

        var userId = UUID.fromString(jwtAuth.getToken().getSubject());

        var command = new ListingCreationService.CreateListingCommand(
                listingCreate.getTitle(),
                listingCreate.getDescription(),
                listingCreate.getListingType().getValue(),
                priceCents,
                listingCreate.getSizeSqm() != null ? BigDecimal.valueOf(listingCreate.getSizeSqm()) : null,
                listingCreate.getRooms(),
                listingCreate.getFloor(),
                listingCreate.getEnergyClass(),
                listingCreate.getAddress(),
                listingCreate.getCity(),
                listingCreate.getPostalCode(),
                geo.getLat(),
                geo.getLng()
        );

        try {
            var listing = listingCreationService.createListingForUser(userId, command);
            var typeCode = listingCreate.getListingType();
            return ResponseEntity.status(HttpStatus.CREATED).body(toApi(listing,ListingStatusesEnum.DRAFT.getDescription(),typeCode.getValue(),List.of()));
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
            @Parameter(name = "id", description = "", required = true, in = ParameterIn.PATH) UUID id
    ){
        var listingDetails = listingCreationService.getListingDetails(id);
        var photos = listingMediaService.getListingPhotos(id);
        return ResponseEntity.status(HttpStatus.OK).body(toApi(listingDetails.listing(), listingDetails.listingStatus().code(),listingDetails.listingType().code(),photos.stream().map(this::toApi).toList()));
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

    private Listing toApi(it.dieti.dietiestatesbackend.domain.listing.Listing listing,String listingStatus, String typeCode,List<ListingPhoto> photos) {
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
        body.setEnergyClass(listing.energyClass());
        body.setFeatures(List.of());
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
