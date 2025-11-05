package it.dieti.dietiestatesbackend.application.listing;

import it.dieti.dietiestatesbackend.application.exception.ApplicationHttpException;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.ConflictException;
import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.application.exception.InternalServerErrorException;
import it.dieti.dietiestatesbackend.application.exception.NotFoundException;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.application.exception.listing.AgentProfileRequiredException;
import it.dieti.dietiestatesbackend.application.exception.listing.CoordinatesValidationException;
import it.dieti.dietiestatesbackend.application.exception.listing.ListingStatusUnavailableException;
import it.dieti.dietiestatesbackend.application.exception.listing.ListingTypeNotSupportedException;
import it.dieti.dietiestatesbackend.application.exception.listing.PriceValidationException;
import it.dieti.dietiestatesbackend.application.feature.FeatureService;
import it.dieti.dietiestatesbackend.application.notification.NotificationService;
import it.dieti.dietiestatesbackend.application.moderation.ModerationService;
import it.dieti.dietiestatesbackend.domain.agent.Agent;
import it.dieti.dietiestatesbackend.domain.agent.AgentRepository;
import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingRepository;
import it.dieti.dietiestatesbackend.domain.listing.ListingType;
import it.dieti.dietiestatesbackend.domain.listing.ListingTypeRepository;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatus;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusRepository;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusesEnum;
import it.dieti.dietiestatesbackend.domain.moderation.ModerationAction;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ListingCreationService {
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final String DEFAULT_CURRENCY = "EUR";
    private static final String DRAFT_STATUS_CODE = ListingStatusesEnum.DRAFT.getDescription();
    private static final String PUBLISHED_STATUS_CODE = ListingStatusesEnum.PUBLISHED.getDescription();
    private static final List<String> SUPPORTED_ENERGY_CLASSES_ORDERED = List.of("A4", "A3", "A2", "A1", "B", "C", "D", "E", "F", "G");
    private static final Set<String> SUPPORTED_ENERGY_CLASSES = Set.copyOf(SUPPORTED_ENERGY_CLASSES_ORDERED);
    private static final String ENERGY_CLASS_FIELD = "energyClass";
    private static final String ENERGY_CLASS_REQUIRED_MESSAGE = "Il campo '" + ENERGY_CLASS_FIELD + "' è obbligatorio.";
    private static final String ENERGY_CLASS_INVALID_MESSAGE = "Valore non valido per 'energyClass'. Valori ammessi: " + String.join(", ", SUPPORTED_ENERGY_CLASSES_ORDERED) + ".";
    private static final String SECURITY_DEPOSIT_NON_NEGATIVE_MESSAGE = "Il campo 'securityDepositCents' deve essere maggiore o uguale a zero.";
    private static final String CONDO_FEE_NON_NEGATIVE_MESSAGE = "Il campo 'condoFeeCents' deve essere maggiore o uguale a zero.";
    public static final String ANNUNCIO = "Annuncio";

    private final ListingRepository listingRepository;
    private final ListingTypeRepository listingTypeRepository;
    private final ListingStatusRepository listingStatusRepository;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FeatureService featureService;
    private final NotificationService notificationService;
    private final ModerationService moderationService;

    private static final Logger log = LoggerFactory.getLogger(ListingCreationService.class);
    private static final String INTERNAL_ERROR_MESSAGE = "Si è verificato un errore interno. Riprova più tardi.";
    private static final String USER_ID_REQUIRED_MESSAGE = "userId is required";
    private static final String LISTING_ID_REQUIRED_MESSAGE = "listingId is required";

    public ListingCreationService(ListingRepository listingRepository,
                                  ListingTypeRepository listingTypeRepository,
                                  ListingStatusRepository listingStatusRepository,
                                  AgentRepository agentRepository,
                                  UserRepository userRepository,
                                  RoleRepository roleRepository,
                                  FeatureService featureService,
                                  NotificationService notificationService,
                                  ModerationService moderationService) {
        this.listingRepository = listingRepository;
        this.listingTypeRepository = listingTypeRepository;
        this.listingStatusRepository = listingStatusRepository;
        this.agentRepository = agentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.featureService = featureService;
        this.notificationService = notificationService;
        this.moderationService = moderationService;
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
            String contractDescription,
            Long securityDepositCents,
            Boolean furnished,
            Long condoFeeCents,
            Boolean petsAllowed,
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
            String contractDescription,
            Long securityDepositCents,
            Boolean furnished,
            Long condoFeeCents,
            Boolean petsAllowed,
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
        Objects.requireNonNull(userId, USER_ID_REQUIRED_MESSAGE);
        Objects.requireNonNull(command, "command is required");

        var userRole = resolveUserRole(userId);
        if (userRole == RolesEnum.ADMIN || userRole == RolesEnum.SUPERADMIN) {
            log.warn("User {} with role {} attempted to create a listing", userId, userRole);
            throw ForbiddenException.actionRequiresRole(RolesEnum.AGENT.name());
        }

        var agent = agentRepository.findByUserId(userId)
                .orElseThrow(AgentProfileRequiredException::new);

        var normalizedInput = normalizeAndValidateCreateInputs(command);
        if (!normalizedInput.fieldErrors().isEmpty()) {
            log.warn("Richiesta listing non valida per user {}: campi mancanti", userId);
            throw BadRequestException.forFields("Richiesta non valida: completare tutti i campi obbligatori.", normalizedInput.fieldErrors());
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
                normalizedInput.title(),
                normalizedInput.description(),
                priceCents,
                DEFAULT_CURRENCY,
                command.sizeSqm(),
                command.rooms(),
                command.floor(),
                normalizedInput.energyClass(),
                normalizedInput.contractDescription(),
                normalizedInput.securityDepositCents(),
                normalizedInput.furnished(),
                normalizedInput.condoFeeCents(),
                normalizedInput.petsAllowed(),
                normalizedInput.addressLine(),
                normalizedInput.city(),
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
        Objects.requireNonNull(userId, USER_ID_REQUIRED_MESSAGE);
        Objects.requireNonNull(listingId, LISTING_ID_REQUIRED_MESSAGE);
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
            ensureOwnership(agent, listing);
        } else {
            log.info("Privileged user {} with role {} updated listing {}", userId, userRole, listingId);
        }

        var updatedListing = applyListingUpdates(listing, command);
        var saved = listingRepository.save(updatedListing);
        featureService.syncListingFeatures(saved.id(), command.featureCodes());
        return saved;
    }

    @Transactional
    public Listing requestDeletion(UUID userId, UUID listingId, String reason) {
        Objects.requireNonNull(userId, USER_ID_REQUIRED_MESSAGE);
        Objects.requireNonNull(listingId, LISTING_ID_REQUIRED_MESSAGE);

        var listing = requireListing(listingId);
        var userRole = resolveUserRole(userId);
        if (userRole == null) {
            log.warn("User {} senza ruolo durante richiesta cancellazione annuncio {}", userId, listingId);
            throw UnauthorizedException.userNotFound();
        }
        boolean isPrivileged = userRole == RolesEnum.ADMIN || userRole == RolesEnum.SUPERADMIN;

        var sanitizedReason = StringUtils.hasText(reason) ? reason.trim() : null;
        if (sanitizedReason != null && sanitizedReason.length() > 500) {
            throw BadRequestException.forField("reason", "Il campo 'reason' non può superare i 500 caratteri.");
        }
        if (isPrivileged && !StringUtils.hasText(sanitizedReason)) {
            throw BadRequestException.forField("reason", "Il campo 'reason' è obbligatorio per gli amministratori.");
        }

        Agent agent = null;
        if (!isPrivileged) {
            agent = requireAgent(userId);
            if (!agent.id().equals(listing.ownerAgentId())) {
                log.warn("Agent {} attempted to delete listing {} not owned", agent.id(), listingId);
                throw ForbiddenException.of("Permesso negato: puoi cancellare solo gli annunci che hai creato.");
            }
        } else {
            agent = agentRepository.findById(listing.ownerAgentId()).orElseThrow(()->{
                log.error("Profilo agente {} non trovato durante delete listing {}", listing.ownerAgentId(),listingId);
                return new InternalServerErrorException(INTERNAL_ERROR_MESSAGE);
            });
        }

        Agent finalAgent = agent;
        var agentUser = userRepository.findById(agent.userId()).orElseThrow(()->{
            log.error("Uutente {} non trovato durante delete listing {}", finalAgent.id(),listingId);
            return new InternalServerErrorException(INTERNAL_ERROR_MESSAGE);
        });

        var listingStatus = listingStatusRepository.findById(listing.statusId())
                .orElseThrow(() -> {
                    log.error("Annuncio {} con status {} inesistente durante delete", listingId, listing.statusId());
                    return new InternalServerErrorException(INTERNAL_ERROR_MESSAGE);
                });

        if (!ListingStatusesEnum.PUBLISHED.getDescription().equals(listingStatus.code())) {
            log.warn("Tentativo di cancellare annuncio {} con stato {}", listingId, listingStatus.code());
            throw BadRequestException.of("Solo gli annunci in stato PUBLISHED possono essere cancellati.");
        }

        var pendingDeleteStatus = listingStatusRepository.findByCode(ListingStatusesEnum.PENDING_DELETE.getDescription())
                .orElseThrow(() -> {
                    log.error("Stato PENDING_DELETE non configurato");
                    return new InternalServerErrorException(INTERNAL_ERROR_MESSAGE);
                });

        var now = OffsetDateTime.now();
        var pendingDeleteUntil = now.plusHours(24);

        var updatedListing = new Listing(
                listing.id(),
                listing.agencyId(),
                listing.ownerAgentId(),
                listing.listingTypeId(),
                pendingDeleteStatus.id(),
                listing.title(),
                listing.description(),
                listing.priceCents(),
                listing.currency(),
                listing.sizeSqm(),
                listing.rooms(),
                listing.floor(),
                listing.energyClass(),
                listing.contractDescription(),
                listing.securityDepositCents(),
                listing.furnished(),
                listing.condoFeeCents(),
                listing.petsAllowed(),
                listing.addressLine(),
                listing.city(),
                listing.postalCode(),
                listing.geo(),
                pendingDeleteUntil,
                listing.deletedAt(),
                listing.publishedAt(),
                listing.createdAt(),
                now
        );
        var savedListing = listingRepository.save(updatedListing);
        moderationService.recordListingDeletion(savedListing.id(), userId, userRole, sanitizedReason);

        if (isPrivileged) {
            notificationService.sendDeleteListing(agentUser.email(), listing.title(), listingId, sanitizedReason);
        } else {
            notificationService.sendDeleteListing(agentUser.email(), listing.title(), listingId, null);
        }
        return savedListing;
    }

    public Listing restoreListing(UUID userId, UUID listingId) {
        Objects.requireNonNull(userId, USER_ID_REQUIRED_MESSAGE);
        Objects.requireNonNull(listingId, LISTING_ID_REQUIRED_MESSAGE);

        var listing = requireListing(listingId);
        var userRole = resolveUserRole(userId);
        if (userRole == null) {
            log.warn("User {} senza ruolo durante ripristino annuncio {}", userId, listingId);
            throw UnauthorizedException.userNotFound();
        }

        var pendingDeleteStatus = listingStatusRepository.findByCode(ListingStatusesEnum.PENDING_DELETE.getDescription())
                .orElseThrow(() -> {
                    log.error("Stato PENDING_DELETE non configurato durante ripristino");
                    return new InternalServerErrorException(INTERNAL_ERROR_MESSAGE);
                });
        if (!pendingDeleteStatus.id().equals(listing.statusId())) {
            log.warn("Tentativo di ripristinare annuncio {} non in stato PENDING_DELETE", listingId);
            throw BadRequestException.of("L'annuncio non è contrassegnato per la cancellazione.");
        }

        if (listing.pendingDeleteUntil() == null) {
            log.error("Annuncio {} in stato PENDING_DELETE senza pendingDeleteUntil", listingId);
            throw ConflictException.of("La finestra di ripristino per l'annuncio è scaduta.");
        }

        var now = OffsetDateTime.now();
        if (now.isAfter(listing.pendingDeleteUntil())) {
            log.warn("Ripristino annuncio {} fallito: finestra scaduta", listingId);
            throw ConflictException.of("La finestra di ripristino per l'annuncio è scaduta.");
        }

        ModerationAction latestDeletion = moderationService.findLatestDeletionAction(listingId)
                .orElseThrow(() -> ConflictException.of("Nessuna cancellazione registrata per l'annuncio."));

        boolean isPrivileged = userRole == RolesEnum.ADMIN || userRole == RolesEnum.SUPERADMIN;
        if (!isPrivileged) {
            var agent = requireAgent(userId);
            ensureOwnership(agent, listing);

            if (latestDeletion.performedByRole() == RolesEnum.ADMIN || latestDeletion.performedByRole() == RolesEnum.SUPERADMIN) {
                log.warn("Agent {} ha tentato di ripristinare annuncio {} cancellato da admin", agent.id(), listingId);
                throw ForbiddenException.of("Permesso negato: l'annuncio è stato cancellato da un amministratore.");
            }
            if (!userId.equals(latestDeletion.performedByUserId())) {
                log.warn("Agent {} ha tentato di ripristinare annuncio {} cancellato da altro utente {}", agent.id(), listingId, latestDeletion.performedByUserId());
                throw ForbiddenException.of("Permesso negato: puoi ripristinare solo gli annunci che hai cancellato tu.");
            }
        }

        var publishedStatus = listingStatusRepository.findByCode(ListingStatusesEnum.PUBLISHED.getDescription())
                .orElseThrow(() -> {
                    log.error("Stato PUBLISHED non configurato durante ripristino");
                    return new InternalServerErrorException(INTERNAL_ERROR_MESSAGE);
                });

        var restoredListing = new Listing(
                listing.id(),
                listing.agencyId(),
                listing.ownerAgentId(),
                listing.listingTypeId(),
                publishedStatus.id(),
                listing.title(),
                listing.description(),
                listing.priceCents(),
                listing.currency(),
                listing.sizeSqm(),
                listing.rooms(),
                listing.floor(),
                listing.energyClass(),
                listing.contractDescription(),
                listing.securityDepositCents(),
                listing.furnished(),
                listing.condoFeeCents(),
                listing.petsAllowed(),
                listing.addressLine(),
                listing.city(),
                listing.postalCode(),
                listing.geo(),
                null,
                listing.deletedAt(),
                listing.publishedAt(),
                listing.createdAt(),
                now
        );

        var savedListing = listingRepository.save(restoredListing);
        moderationService.recordListingRestoration(savedListing.id(), userId, userRole);
        return savedListing;
    }

    private Agent requireAgent(UUID userId) {
        return agentRepository.findByUserId(userId)
                .orElseThrow(AgentProfileRequiredException::new);
    }

    private Listing requireListing(UUID listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> {
                    log.warn("Annuncio {} non trovato durante aggiornamento", listingId);
                    return NotFoundException.resourceNotFound(ANNUNCIO, listingId);
                });
    }

    private void ensureOwnership(Agent agent, Listing listing) {
        if (!agent.id().equals(listing.ownerAgentId())) {
            log.warn("Agent {} attempted to update listing {} not owned", agent.id(), listing.id());
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
        var energyClass = resolveUpdatedEnergyClass(command.energyClass(), listing.energyClass());
        var contractDescription = command.contractDescription() != null
                ? normalizeOptional(command.contractDescription())
                : listing.contractDescription();
        var securityDepositCents = resolveUpdatedNonNegativeAmount(
                listing.securityDepositCents(),
                command.securityDepositCents(),
                "securityDepositCents",
                SECURITY_DEPOSIT_NON_NEGATIVE_MESSAGE
        );
        var furnished = command.furnished() != null ? command.furnished() : listing.furnished();
        var condoFeeCents = resolveUpdatedNonNegativeAmount(
                listing.condoFeeCents(),
                command.condoFeeCents(),
                "condoFeeCents",
                CONDO_FEE_NON_NEGATIVE_MESSAGE
        );
        var petsAllowed = command.petsAllowed() != null ? command.petsAllowed() : listing.petsAllowed();
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
                contractDescription,
                securityDepositCents,
                furnished,
                condoFeeCents,
                petsAllowed,
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

    private long resolveUpdatedNonNegativeAmount(long currentValue, Long requestedValue, String fieldName, String message) {
        if (requestedValue == null) {
            return currentValue;
        }
        if (requestedValue < 0) {
            throw BadRequestException.forField(fieldName, message);
        }
        return requestedValue;
    }

    private CreateListingValidation normalizeAndValidateCreateInputs(CreateListingCommand command) {
        var title = normalize(command.title());
        var description = normalize(command.description());
        var addressLine = normalize(command.addressLine());
        var city = normalize(command.city());
        var energyClassRaw = normalize(command.energyClass());
        var contractDescription = normalizeOptional(command.contractDescription());
        long securityDepositCents = command.securityDepositCents() != null ? command.securityDepositCents() : 0L;
        long condoFeeCents = command.condoFeeCents() != null ? command.condoFeeCents() : 0L;
        boolean furnished = Boolean.TRUE.equals(command.furnished());
        boolean petsAllowed = Boolean.TRUE.equals(command.petsAllowed());

        List<ApplicationHttpException.FieldErrorDetail> errors = new ArrayList<>();
        if (title.isBlank()) {
            errors.add(new ApplicationHttpException.FieldErrorDetail("title", "Il campo 'title' è obbligatorio."));
        }
        if (description.isBlank()) {
            errors.add(new ApplicationHttpException.FieldErrorDetail("description", "Il campo 'description' è obbligatorio."));
        }
        if (addressLine.isBlank()) {
            errors.add(new ApplicationHttpException.FieldErrorDetail("address", "Il campo 'address' è obbligatorio."));
        }
        if (city.isBlank()) {
            errors.add(new ApplicationHttpException.FieldErrorDetail("city", "Il campo 'city' è obbligatorio."));
        }

        String normalizedEnergyClass = null;
        if (energyClassRaw.isBlank()) {
            errors.add(new ApplicationHttpException.FieldErrorDetail(ENERGY_CLASS_FIELD, ENERGY_CLASS_REQUIRED_MESSAGE));
        } else {
            var candidate = energyClassRaw.toUpperCase(Locale.ROOT);
            if (!SUPPORTED_ENERGY_CLASSES.contains(candidate)) {
                errors.add(new ApplicationHttpException.FieldErrorDetail(ENERGY_CLASS_FIELD, ENERGY_CLASS_INVALID_MESSAGE));
            } else {
                normalizedEnergyClass = candidate;
            }
        }

        if (securityDepositCents < 0) {
            errors.add(new ApplicationHttpException.FieldErrorDetail("securityDepositCents", SECURITY_DEPOSIT_NON_NEGATIVE_MESSAGE));
        }
        if (condoFeeCents < 0) {
            errors.add(new ApplicationHttpException.FieldErrorDetail("condoFeeCents", CONDO_FEE_NON_NEGATIVE_MESSAGE));
        }

        return new CreateListingValidation(
                title,
                description,
                addressLine,
                city,
                normalizedEnergyClass,
                contractDescription,
                securityDepositCents,
                furnished,
                condoFeeCents,
                petsAllowed,
                List.copyOf(errors)
        );
    }

    public ListingDetails getListingDetails(UUID listingId, UUID userId) {
        Objects.requireNonNull(listingId, LISTING_ID_REQUIRED_MESSAGE);

        var listing = listingRepository.findById(listingId)
                .orElseThrow(() -> {
                    log.warn("Annuncio {} non trovato durante recupero dettagli", listingId);
                    return NotFoundException.resourceNotFound(ANNUNCIO, listingId);
                });

        ListingType listingType = null;
        if (listing.listingTypeId() != null) {
            listingType = listingTypeRepository.findById(listing.listingTypeId())
                    .orElseThrow(() -> {
                        log.error("Annuncio '{}' con listingType '{}' inesistente", listingId, listing.listingTypeId());
                    return new InternalServerErrorException(INTERNAL_ERROR_MESSAGE);
                    });
        }

        ListingStatus listingStatus = null;
        if (listing.statusId() != null) {
            listingStatus = listingStatusRepository.findById(listing.statusId())
                    .orElseThrow(() -> {
                        log.error("Annuncio {} con status {} inesistente", listingId, listing.statusId());
                        return new InternalServerErrorException(INTERNAL_ERROR_MESSAGE);
                    });
        }

        var isListingPublic = listingStatus!= null && listingStatus.code().equals(ListingStatusesEnum.PUBLISHED.getDescription());
        if (userId != null){
            var userRole = resolveUserRole(userId);
            boolean isPrivileged = userRole == RolesEnum.ADMIN || userRole == RolesEnum.SUPERADMIN;
            if (!isPrivileged && !isListingPublic) {
                try {
                    var agent = requireAgent(userId);
                    ensureOwnership(agent,listing);
                } catch (Exception e) {
                    throw NotFoundException.resourceNotFound(ANNUNCIO, listingId);
                }
            }
        } else {
            if (!isListingPublic) {
                throw NotFoundException.resourceNotFound(ANNUNCIO, listingId);
            }
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

    private String resolveUpdatedEnergyClass(String requestedEnergyClass, String currentEnergyClass) {
        if (requestedEnergyClass == null) {
            return currentEnergyClass;
        }
        var normalized = normalize(requestedEnergyClass);
        if (normalized.isEmpty()) {
            throw BadRequestException.forField(ENERGY_CLASS_FIELD, ENERGY_CLASS_REQUIRED_MESSAGE);
        }
        var candidate = normalized.toUpperCase(Locale.ROOT);
        if (!SUPPORTED_ENERGY_CLASSES.contains(candidate)) {
            throw BadRequestException.forField(ENERGY_CLASS_FIELD, ENERGY_CLASS_INVALID_MESSAGE);
        }
        return candidate;
    }

    private String normalizeOptional(String value) {
        var normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private record CreateListingValidation(
            String title,
            String description,
            String addressLine,
            String city,
            String energyClass,
            String contractDescription,
            long securityDepositCents,
            boolean furnished,
            long condoFeeCents,
            boolean petsAllowed,
            List<ApplicationHttpException.FieldErrorDetail> fieldErrors
    ) {}
}
