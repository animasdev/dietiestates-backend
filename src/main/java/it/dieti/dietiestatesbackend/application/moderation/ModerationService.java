package it.dieti.dietiestatesbackend.application.moderation;

import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.application.exception.NotFoundException;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.domain.agent.AgentRepository;
import it.dieti.dietiestatesbackend.domain.agency.AgencyRepository;
import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingRepository;
import it.dieti.dietiestatesbackend.domain.moderation.ModerationAction;
import it.dieti.dietiestatesbackend.domain.moderation.ModerationActionRepository;
import it.dieti.dietiestatesbackend.domain.moderation.ModerationActionType;
import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RoleRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RolesEnum;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ModerationService {

    private static final List<String> ALLOWED_ROLES = List.of(
            RolesEnum.ADMIN.name(),
            RolesEnum.SUPERADMIN.name(),
            RolesEnum.AGENCY.name(),
            RolesEnum.AGENT.name()
    );
    public static final String LISTING_ID_IS_REQUIRED = "listingId is required";

    private final ModerationActionRepository moderationActionRepository;
    private final ListingRepository listingRepository;
    private final AgentRepository agentRepository;
    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public ModerationService(ModerationActionRepository moderationActionRepository,
                             ListingRepository listingRepository,
                             AgentRepository agentRepository,
                             AgencyRepository agencyRepository,
                             UserRepository userRepository,
                             RoleRepository roleRepository) {
        this.moderationActionRepository = moderationActionRepository;
        this.listingRepository = listingRepository;
        this.agentRepository = agentRepository;
        this.agencyRepository = agencyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public void recordListingDeletion(UUID listingId, UUID performedByUserId, RolesEnum performedByRole, String reason) {
        Objects.requireNonNull(listingId, LISTING_ID_IS_REQUIRED);
        Objects.requireNonNull(performedByUserId, "performedByUserId is required");
        Objects.requireNonNull(performedByRole, "performedByRole is required");

        var sanitizedReason = StringUtils.hasText(reason) ? reason.trim() : null;
        var action = new ModerationAction(
                null,
                listingId,
                performedByUserId,
                performedByRole,
                ModerationActionType.DELETE,
                sanitizedReason,
                OffsetDateTime.now()
        );
        moderationActionRepository.save(action);
    }

    public void recordListingRestoration(UUID listingId, UUID performedByUserId, RolesEnum performedByRole) {
        Objects.requireNonNull(listingId, LISTING_ID_IS_REQUIRED);
        Objects.requireNonNull(performedByUserId, "performedByUserId is required");
        Objects.requireNonNull(performedByRole, "performedByRole is required");

        var action = new ModerationAction(
                null,
                listingId,
                performedByUserId,
                performedByRole,
                ModerationActionType.RESTORE,
                null,
                OffsetDateTime.now()
        );
        moderationActionRepository.save(action);
    }

    public Optional<ModerationAction> findLatestDeletionAction(UUID listingId) {
        Objects.requireNonNull(listingId, LISTING_ID_IS_REQUIRED);
        return moderationActionRepository.findByListingId(listingId).stream()
                .filter(action -> action.actionType() == ModerationActionType.DELETE)
                .findFirst();
    }

    public List<ModerationLogEntry> getModerationActions(UUID requesterUserId, UUID listingId) {
        Objects.requireNonNull(requesterUserId, "requesterUserId is required");

        var role = resolveUserRole(requesterUserId);
        if (role == null) {
            throw UnauthorizedException.userNotFound();
        }

        if (role == RolesEnum.ADMIN || role == RolesEnum.SUPERADMIN) {
            return mapToView(listingId != null
                    ? moderationActionRepository.findByListingId(listingId)
                    : moderationActionRepository.findAll());
        }

        if (role == RolesEnum.AGENT) {
            var agent = agentRepository.findByUserId(requesterUserId)
                    .orElseThrow(() -> ForbiddenException.actionRequiresRole(RolesEnum.AGENT.name()));

            if (listingId != null) {
                ensureListingOwnedByAgent(agent.id(), listingId);
                return mapToView(moderationActionRepository.findByListingId(listingId));
            }

            var listings = listingRepository.findAllByOwnerAgentId(agent.id());
            return mapToView(moderationActionRepository.findByListingIds(extractIds(listings)));
        }

        if (role == RolesEnum.AGENCY) {
            var agency = agencyRepository.findByUserId(requesterUserId)
                    .orElseThrow(() -> ForbiddenException.actionRequiresRole(RolesEnum.AGENCY.name()));

            if (listingId != null) {
                ensureListingBelongsToAgency(agency.id(), listingId);
                return mapToView(moderationActionRepository.findByListingId(listingId));
            }

            var listings = listingRepository.findAllByAgencyId(agency.id());
            return mapToView(moderationActionRepository.findByListingIds(extractIds(listings)));
        }

        throw ForbiddenException.actionRequiresRoles(ALLOWED_ROLES);
    }

    private RolesEnum resolveUserRole(UUID userId) {
        var user = userRepository.findById(userId).orElse(null);
        if (user == null || user.roleId() == null) {
            return null;
        }
        var role = roleRepository.findById(user.roleId()).orElse(null);
        if (role == null) {
            return null;
        }
        try {
            return RolesEnum.valueOf(role.code());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void ensureListingOwnedByAgent(UUID agentId, UUID listingId) {
        var listing = listingRepository.findById(listingId)
                .orElseThrow(() -> NotFoundException.resourceNotFound("Annuncio", listingId));
        if (!agentId.equals(listing.ownerAgentId())) {
            throw ForbiddenException.of("Permesso negato: puoi visualizzare solo le moderazioni dei tuoi annunci.");
        }
    }

    private void ensureListingBelongsToAgency(UUID agencyId, UUID listingId) {
        var listing = listingRepository.findById(listingId)
                .orElseThrow(() -> NotFoundException.resourceNotFound("Annuncio", listingId));
        if (!agencyId.equals(listing.agencyId())) {
            throw ForbiddenException.of("Permesso negato: l'annuncio non appartiene alla tua agenzia.");
        }
    }

    private List<UUID> extractIds(List<Listing> listings) {
        return listings.stream()
                .map(Listing::id)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<ModerationLogEntry> mapToView(List<ModerationAction> actions) {
        return actions.stream()
                .map(action -> new ModerationLogEntry(
                        action.id(),
                        action.listingId(),
                        action.performedByUserId(),
                        action.performedByRole(),
                        action.actionType(),
                        action.reason(),
                        action.createdAt()
                ))
                .toList();
    }

    public record ModerationLogEntry(
            UUID id,
            UUID listingId,
            UUID performedByUserId,
            RolesEnum performedByRole,
            ModerationActionType actionType,
            String reason,
            OffsetDateTime createdAt
    ) {
    }
}
