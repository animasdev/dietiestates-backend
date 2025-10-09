package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.Listing;
import it.dieti.dietiestatesbackend.api.model.ListingCreate;
import it.dieti.dietiestatesbackend.api.model.ListingGeo;
import it.dieti.dietiestatesbackend.application.exception.ApplicationHttpException;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.InternalServerErrorException;
import it.dieti.dietiestatesbackend.application.exception.listing.AgentProfileRequiredException;
import it.dieti.dietiestatesbackend.application.exception.listing.CoordinatesValidationException;
import it.dieti.dietiestatesbackend.application.exception.listing.ListingStatusUnavailableException;
import it.dieti.dietiestatesbackend.application.exception.listing.ListingTypeNotSupportedException;
import it.dieti.dietiestatesbackend.application.exception.listing.PriceValidationException;
import it.dieti.dietiestatesbackend.application.listing.ListingCreationService;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ListingsApiDelegateImpl implements ListingsApiDelegate {

    private final ListingCreationService listingCreationService;
    private static final Logger log = LoggerFactory.getLogger(ListingsApiDelegateImpl.class);

    public ListingsApiDelegateImpl(ListingCreationService listingCreationService) {
        this.listingCreationService = listingCreationService;
    }

    @Override
    public ResponseEntity<Listing> listingsPost(ListingCreate listingCreate) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (listingCreate.getListingType() == null) {
            var message = "Il campo 'listing_type' è obbligatorio.";
            throw BadRequestException.forField("listing_type", message);
        }
        if (listingCreate.getGeo() == null) {
            var message = "Il campo 'geo' è obbligatorio.";
            throw BadRequestException.forField("geo", message);
        }

        var geo = listingCreate.getGeo();
        if (geo.getLat() == null) {
            var message = "Il campo 'geo.lat' è obbligatorio.";
            throw BadRequestException.forField("geo.lat", message);
        }
        if (geo.getLng() == null) {
            var message = "Il campo 'geo.lng' è obbligatorio.";
            throw BadRequestException.forField("geo.lng", message);
        }

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
            return ResponseEntity.status(HttpStatus.CREATED).body(toApi(listing, typeCode.getValue()));
        } catch (ListingTypeNotSupportedException ex) {
            var acceptedTypes = Arrays.stream(ListingCreate.ListingTypeEnum.values())
                    .map(ListingCreate.ListingTypeEnum::getValue)
                    .collect(Collectors.joining(", "));
            var message = "Listing type non supportato. Tipi ammessi: " + acceptedTypes + ".";
            log.warn("Listing type non supportato ({}) per user {}", ex.requestedType(), userId);
            throw BadRequestException.forField("listing_type", message);
        } catch (ListingStatusUnavailableException ex) {
            log.error("Listing status {} non disponibile durante la creazione (user {})", ex.statusCode(), userId, ex);
            throw ex;
        } catch (AgentProfileRequiredException | PriceValidationException | CoordinatesValidationException ex) {
            log.warn("Errore validazione business per user {}: {}", userId, ex.getMessage());
            throw ex;
        } catch (ApplicationHttpException ex) {
            log.warn("Errore applicativo nella creazione listing per user {}: {}", userId, ex.getMessage());
            throw ex;
        } catch (IllegalArgumentException ex) {
            log.warn("Richiesta listing non valida per user {}: {}", userId, ex.getMessage());
            throw BadRequestException.of(ex.getMessage());
        } catch (IllegalStateException ex) {
            log.error("Errore interno durante la creazione listing per user {}", userId, ex);
            throw new InternalServerErrorException("Si è verificato un errore interno. Riprova più tardi.");
        }
    }

    private Listing toApi(it.dieti.dietiestatesbackend.domain.listing.Listing listing, String typeCode) {
        Listing body = new Listing();
        body.setId(listing.id());
        body.setAgencyId(listing.agencyId());
        body.setOwnerAgentId(listing.ownerAgentId());
        body.setListingType(Listing.ListingTypeEnum.valueOf(typeCode));
        body.setStatus(Listing.StatusEnum.valueOf(ListingStatusesEnum.DRAFT.getDescription()));
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
        body.setPhotos(List.of());
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
