package it.dieti.dietiestatesbackend.application.listing;

import it.dieti.dietiestatesbackend.application.exception.ApplicationHttpException;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.application.exception.InternalServerErrorException;
import it.dieti.dietiestatesbackend.application.exception.NotFoundException;
import it.dieti.dietiestatesbackend.application.exception.listing.AgentProfileRequiredException;
import it.dieti.dietiestatesbackend.application.exception.listing.CoordinatesValidationException;
import it.dieti.dietiestatesbackend.application.exception.listing.ListingStatusUnavailableException;
import it.dieti.dietiestatesbackend.application.exception.listing.ListingTypeNotSupportedException;
import it.dieti.dietiestatesbackend.application.exception.listing.PriceValidationException;
import it.dieti.dietiestatesbackend.application.feature.FeatureService;
import it.dieti.dietiestatesbackend.domain.agent.Agent;
import it.dieti.dietiestatesbackend.domain.agent.AgentRepository;
import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingRepository;
import it.dieti.dietiestatesbackend.domain.listing.ListingTypeRepository;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusRepository;
import it.dieti.dietiestatesbackend.domain.listing.ListingType;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatus;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusesEnum;
import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RoleRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RolesEnum;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
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
    private static final String DRAFT_STATUS_CODE = ListingStatusesEnum.DRAFT.getDescription();
    private static final String PUBLISHED_STATUS_CODE = ListingStatusesEnum.PUBLISHED.getDescription();

    private final ListingRepository listingRepository;
    private final ListingTypeRepository listingTypeRepository;
    private final ListingStatusRepository listingStatusRepository;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FeatureService featureService;
    private static final Logger log = LoggerFactory.getLogger(ListingCreationService.class);

    public ListingCreationService(ListingRepository listingRepository,
                                  ListingTypeRepository listingTypeRepository,
                                  ListingStatusRepository listingStatusRepository,
                                  AgentRepository agentRepository,
                                  UserRepository userRepository,
                                  RoleRepository roleRepository,
                                  FeatureService featureService) {
        this.listingRepository = listingRepository;
        this.listingTypeRepository = listingTypeRepository;
        this.listingStatusRepository = listingStatusRepository;
        this.agentRepository = agentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.featureService = featureService;
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
            double longitude,
            boolean isPublished,
            List<String> featureCodes
    ) {}

    public record UpdateListingCommand(
            String title,
            String description,
            Long priceCents,
            BigDecimal sizeSqm,
            Integer rooms,
            Integer floor,
            String energyClass,
            String addressLine,
            String city,
            String postalCode,
            Double latitude,
            Double longitude,
            List<String> featureCodes
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

        var userRole = resolveUserRole(userId);
        if (userRole == RolesEnum.ADMIN || userRole == RolesEnum.SUPERADMIN) {
            log.warn("User {} with role {} attempted to create a listing", userId, userRole);
            throw ForbiddenException.actionRequiresRole(RolesEnum.AGENT.name());
        }

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

        var statusString = command.isPublished() ? PUBLISHED_STATUS_CODE : DRAFT_STATUS_CODE;
        var status = listingStatusRepository.findByCode(statusString)
                .orElseThrow(() -> new ListingStatusUnavailableException(statusString));

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
        log.info("Listing created {}", listing);
        var savedListing = listingRepository.save(listing);
        featureService.syncListingFeatures(savedListing.id(), command.featureCodes());
        return savedListing;
    }

    @Transactional
    public Listing updateListingForUser(UUID userId, UUID listingId, UpdateListingCommand command) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(listingId, "listingId is required");
        Objects.requireNonNull(command, "command is required");

        var userRole = resolveUserRole(userId);
        boolean isPrivileged = userRole == RolesEnum.ADMIN || userRole == RolesEnum.SUPERADMIN;

        Agent agent = null;
        if (isPrivileged) {
            agent = agentRepository.findByUserId(userId).orElse(null);
        } else {
            agent = requireAgent(userId);
        }
        var listing = requireListing(listingId);
        if (!isPrivileged) {
            ensureOwnership(agent, listing, listingId);
        } else {
            log.info("Privileged user {} with role {} updated listing {}", userId, userRole, listingId);
        }

        var updatedListing = applyListingUpdates(listing, command);
        var saved = listingRepository.save(updatedListing);
        featureService.syncListingFeatures(saved.id(), command.featureCodes());
        return saved;
    }

    private Agent requireAgent(UUID userId) {
        return agentRepository.findByUserId(userId)
                .orElseThrow(AgentProfileRequiredException::new);
    }

    private Listing requireListing(UUID listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> {
                    log.warn("Annuncio {} non trovato durante aggiornamento", listingId);
                    return NotFoundException.resourceNotFound("Annuncio", listingId);
                });
    }

    private void ensureOwnership(Agent agent, Listing listing, UUID listingId) {
        if (!agent.id().equals(listing.ownerAgentId())) {
            log.warn("Agent {} attempted to update listing {} not owned", agent.id(), listingId);
            throw ForbiddenException.of("Permesso negato: puoi modificare solo gli annunci che hai creato.");
        }
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
        return roleOpt
                .map(role -> {
                    try {
                        return RolesEnum.valueOf(role.code());
                    } catch (IllegalArgumentException ex) {
                        log.warn("Unsupported role code {} for user {}", role.code(), userId, ex);
                        return null;
                    }
                })
                .orElse(null);
    }

    private Listing applyListingUpdates(Listing listing, UpdateListingCommand command) {
        var title = resolveRequiredTextField(command.title(), listing.title(), "title", "Il campo 'title' non può essere vuoto.");
        var description = resolveRequiredTextField(command.description(), listing.description(), "description", "Il campo 'description' non può essere vuoto.");
        var addressLine = resolveRequiredTextField(command.addressLine(), listing.addressLine(), "address", "Il campo 'address' non può essere vuoto.");
        var city = resolveRequiredTextField(command.city(), listing.city(), "city", "Il campo 'city' non può essere vuoto.");
        var priceCents = resolveUpdatedPrice(listing.priceCents(), command.priceCents());
        var sizeSqm = command.sizeSqm() != null ? command.sizeSqm() : listing.sizeSqm();
        var rooms = command.rooms() != null ? command.rooms() : listing.rooms();
        var floor = command.floor() != null ? command.floor() : listing.floor();
        var energyClass = command.energyClass() != null ? normalizeOptional(command.energyClass()) : listing.energyClass();
        var postalCode = command.postalCode() != null ? normalizeOptional(command.postalCode()) : listing.postalCode();
        var geo = resolveUpdatedGeo(listing.geo(), command.latitude(), command.longitude());

        return new Listing(
                listing.id(),
                listing.agencyId(),
                listing.ownerAgentId(),
                listing.listingTypeId(),
                listing.statusId(),
                title,
                description,
                priceCents,
                listing.currency(),
                sizeSqm,
                rooms,
                floor,
                energyClass,
                addressLine,
                city,
                postalCode,
                geo,
                listing.pendingDeleteUntil(),
                listing.deletedAt(),
                listing.publishedAt(),
                listing.createdAt(),
                listing.updatedAt()
        );
    }

    private String resolveRequiredTextField(String requestedValue, String currentValue, String fieldName, String emptyMessage) {
        if (requestedValue == null) {
            return currentValue;
        }
        var normalized = normalize(requestedValue);
        if (normalized.isBlank()) {
            throw BadRequestException.forField(fieldName, emptyMessage);
        }
        return normalized;
    }

    private long resolveUpdatedPrice(long currentPrice, Long requestedPrice) {
        if (requestedPrice == null) {
            return currentPrice;
        }
        if (requestedPrice < 0) {
            throw PriceValidationException.mustBePositive();
        }
        return requestedPrice;
    }

    private Point resolveUpdatedGeo(Point currentGeo, Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return currentGeo;
        }
        if (latitude == null || longitude == null) {
            throw BadRequestException.forField("geo", "Per aggiornare la posizione devi fornire sia lat che lng.");
        }
        validateCoordinates(latitude, longitude);
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
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
