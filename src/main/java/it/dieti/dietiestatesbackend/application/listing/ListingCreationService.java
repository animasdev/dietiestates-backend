package it.dieti.dietiestatesbackend.application.listing;

import it.dieti.dietiestatesbackend.application.exception.ApplicationHttpException;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.InternalServerErrorException;
import it.dieti.dietiestatesbackend.application.exception.NotFoundException;
import it.dieti.dietiestatesbackend.application.exception.listing.AgentProfileRequiredException;
import it.dieti.dietiestatesbackend.application.exception.listing.CoordinatesValidationException;
import it.dieti.dietiestatesbackend.application.exception.listing.ListingStatusUnavailableException;
import it.dieti.dietiestatesbackend.application.exception.listing.ListingTypeNotSupportedException;
import it.dieti.dietiestatesbackend.application.exception.listing.PriceValidationException;
import it.dieti.dietiestatesbackend.domain.agent.AgentRepository;
import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingRepository;
import it.dieti.dietiestatesbackend.domain.listing.ListingTypeRepository;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusRepository;
import it.dieti.dietiestatesbackend.domain.listing.ListingType;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatus;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class ListingCreationService {
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final String DEFAULT_CURRENCY = "EUR";
    private static final String DEFAULT_STATUS_CODE = "DRAFT";

    private final ListingRepository listingRepository;
    private final ListingTypeRepository listingTypeRepository;
    private final ListingStatusRepository listingStatusRepository;
    private final AgentRepository agentRepository;
    private static final Logger log = LoggerFactory.getLogger(ListingCreationService.class);

    public ListingCreationService(ListingRepository listingRepository,
                                  ListingTypeRepository listingTypeRepository,
                                  ListingStatusRepository listingStatusRepository,
                                  AgentRepository agentRepository) {
        this.listingRepository = listingRepository;
        this.listingTypeRepository = listingTypeRepository;
        this.listingStatusRepository = listingStatusRepository;
        this.agentRepository = agentRepository;
    }

    public record CreateListingCommand(
            String title,
            String description,
            String listingTypeCode,
            Long priceCents,
            BigDecimal sizeSqm,
            Integer rooms,
            Integer floor,
            String energyClass,
            String addressLine,
            String city,
            String postalCode,
            double latitude,
            double longitude
    ) {}

    public record ListingDetails(
            it.dieti.dietiestatesbackend.domain.listing.Listing listing,
            ListingType listingType,
            ListingStatus listingStatus
    ) {}

    @Transactional
    public Listing createListingForUser(UUID userId, CreateListingCommand command) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(command, "command is required");

        var agent = agentRepository.findByUserId(userId)
                .orElseThrow(AgentProfileRequiredException::new);

        var title = normalize(command.title());
        var description = normalize(command.description());
        var addressLine = normalize(command.addressLine());
        var city = normalize(command.city());
        List<ApplicationHttpException.FieldErrorDetail> fieldErrors = new ArrayList<>();
        if (title.isBlank()) {
            fieldErrors.add(new ApplicationHttpException.FieldErrorDetail("title", "Il campo 'title' è obbligatorio."));
        }
        if (description.isBlank()) {
            fieldErrors.add(new ApplicationHttpException.FieldErrorDetail("description", "Il campo 'description' è obbligatorio."));
        }
        if (addressLine.isBlank()) {
            fieldErrors.add(new ApplicationHttpException.FieldErrorDetail("address", "Il campo 'address' è obbligatorio."));
        }
        if (city.isBlank()) {
            fieldErrors.add(new ApplicationHttpException.FieldErrorDetail("city", "Il campo 'city' è obbligatorio."));
        }
        if (!fieldErrors.isEmpty()) {
            log.warn("Richiesta listing non valida per user {}: campi mancanti", userId);
            throw BadRequestException.forFields("Richiesta non valida: completare tutti i campi obbligatori.", fieldErrors);
        }


        var typeCode = normalize(command.listingTypeCode()).toUpperCase(Locale.ROOT);
        var listingType = listingTypeRepository.findByCode(typeCode)
                .orElseThrow(() -> new ListingTypeNotSupportedException(typeCode));

        var status = listingStatusRepository.findByCode(DEFAULT_STATUS_CODE)
                .orElseThrow(() -> new ListingStatusUnavailableException(DEFAULT_STATUS_CODE));

        Long priceValue = command.priceCents();
        if (priceValue == null) {
            throw PriceValidationException.required();
        }
        long priceCents = priceValue;
        if (priceCents < 0) {
            throw PriceValidationException.mustBePositive();
        }

        validateCoordinates(command.latitude(), command.longitude());
        var geo = GEOMETRY_FACTORY.createPoint(new Coordinate(command.longitude(), command.latitude()));

        var listing = new Listing(
                null,
                agent.agencyId(),
                agent.id(),
                listingType.id(),
                status.id(),
                title,
                description,
                priceCents,
                DEFAULT_CURRENCY,
                command.sizeSqm(),
                command.rooms(),
                command.floor(),
                normalizeOptional(command.energyClass()),
                addressLine,
                city,
                normalizeOptional(command.postalCode()),
                geo,
                null,
                null,
                null,
                null,
                null
        );

        return listingRepository.save(listing);
    }

    public ListingDetails getListingDetails(UUID listingId) {
        Objects.requireNonNull(listingId, "listingId is required");

        var listing = listingRepository.findById(listingId)
                .orElseThrow(() -> {
                    log.warn("Annuncio {} non trovato durante recupero dettagli", listingId);
                    return NotFoundException.resourceNotFound("Annuncio", listingId);
                });

        ListingType listingType = null;
        if (listing.listingTypeId() != null) {
            listingType = listingTypeRepository.findById(listing.listingTypeId())
                    .orElseThrow(() -> {
                        log.error("Annuncio '{}' con listingType '{}' inesistente", listingId, listing.listingTypeId());
                        return new InternalServerErrorException("Si è verificato un errore interno. Riprova più tardi.");
                    });
        }

        ListingStatus listingStatus = null;
        if (listing.statusId() != null) {
            listingStatus = listingStatusRepository.findById(listing.statusId())
                    .orElseThrow(() -> {
                        log.error("Annuncio {} con status {} inesistente", listingId, listing.statusId());
                        return new InternalServerErrorException("Si è verificato un errore interno. Riprova più tardi.");
                    });
        }

        return new ListingDetails(listing, listingType, listingStatus);
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (Double.isNaN(latitude)) {
            throw CoordinatesValidationException.notANumber("geo.lat");
        }
        if (Double.isNaN(longitude)) {
            throw CoordinatesValidationException.notANumber("geo.lng");
        }
        if (latitude < -90 || latitude > 90) {
            throw CoordinatesValidationException.latitudeOutOfRange();
        }
        if (longitude < -180 || longitude > 180) {
            throw CoordinatesValidationException.longitudeOutOfRange();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value) {
        var normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
