package it.dieti.dietiestatesbackend.application.user;

import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.domain.agency.Agency;
import it.dieti.dietiestatesbackend.domain.agency.AgencyRepository;
import it.dieti.dietiestatesbackend.domain.agent.Agent;
import it.dieti.dietiestatesbackend.domain.agent.AgentRepository;
import it.dieti.dietiestatesbackend.domain.media.MediaAsset;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetRepository;
import it.dieti.dietiestatesbackend.domain.user.User;
import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import it.dieti.dietiestatesbackend.domain.user.role.Role;
import it.dieti.dietiestatesbackend.domain.user.role.RoleRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RolesEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserDirectoryService {
    private static final Logger log = LoggerFactory.getLogger(UserDirectoryService.class);
    public static final String UPDATED_AT = "updatedAt";
    public static final String CREATED_AT = "createdAt";
    public static final String DISPLAY_NAME = "displayName";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AgencyRepository agencyRepository;
    private final AgentRepository agentRepository;
    private final MediaAssetRepository mediaAssetRepository;

    public UserDirectoryService(UserRepository userRepository,
                                RoleRepository roleRepository,
                                AgencyRepository agencyRepository,
                                AgentRepository agentRepository,
                                MediaAssetRepository mediaAssetRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.agencyRepository = agencyRepository;
        this.agentRepository = agentRepository;
        this.mediaAssetRepository = mediaAssetRepository;
    }

    public PagedResult<AdminAccount> listAdminAccounts(AdminDirectoryQuery query) {
        var codes = List.of(RolesEnum.SUPERADMIN.getDescription(), RolesEnum.ADMIN.getDescription());
        var users = userRepository.findByRoleCodes(codes);
        var roleMap = resolveRoleCodes();

        List<AdminAccount> accounts = users.stream()
                .map(user -> new AdminAccount(
                        user.id(),
                        Objects.toString(user.displayName(), ""),
                        Objects.toString(user.email(), ""),
                        roleMap.getOrDefault(user.roleId(), "UNKNOWN"),
                        user.firstAccess(),
                        user.createdAt(),
                        user.updatedAt()
                ))
                .toList();

        String search = query.search();
        if (search != null) {
            accounts = accounts.stream()
                    .filter(account -> contains(account.displayName(), search) || contains(account.email(), search))
                    .toList();
        }

        if (query.roleFilter() != null) {
            accounts = accounts.stream()
                    .filter(account -> query.roleFilter().name().equalsIgnoreCase(account.role()))
                    .toList();
        }

        List<SortCriterion> sortCriteria = ensureSort(query.sort(), new SortCriterion(DISPLAY_NAME, true));
        Comparator<AdminAccount> comparator = buildAdminComparator(sortCriteria);
        List<AdminAccount> sorted = accounts.stream()
                .sorted(comparator)
                .toList();

        return paginate(sorted, query.page(), query.size(), sortCriteria);
    }

    public PagedResult<AgencyEntry> listAgencies(AgencyDirectoryQuery query) {
        var agencies = agencyRepository.findAll();
        if (agencies.isEmpty()) {
            return new PagedResult<>(List.of(), query.page(), query.size(), 0, sortTokens(query.sort()));
        }

        var ownerIds = agencies.stream()
                .map(Agency::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        var owners = ownerIds.isEmpty() ? Map.<UUID, User>of() : userRepository.findByIds(ownerIds).stream()
                .collect(Collectors.toMap(User::id, user -> user));

        List<AgencyEntry> entries = agencies.stream()
                .map(agency -> {
                    User owner = owners.get(agency.userId());
                    String logoUrl = resolveMediaUrl(agency.logoMediaId());
                    return new AgencyEntry(
                            agency.id(),
                            Objects.toString(agency.name(), ""),
                            Objects.toString(agency.description(), ""),
                            agency.userId(),
                            owner != null ? Objects.toString(owner.displayName(), "") : "",
                            owner != null ? Objects.toString(owner.email(), "") : "",
                            logoUrl,
                            agency.approvedBy(),
                            agency.approvedAt(),
                            agency.createdAt(),
                            agency.updatedAt()
                    );
                })
                .toList();

        String search = query.search();
        if (search != null) {
            entries = entries.stream()
                    .filter(entry -> contains(entry.name(), search)
                            || contains(entry.ownerDisplayName(), search)
                            || contains(entry.ownerEmail(), search))
                    .toList();
        }

        if (query.approved() != null) {
            boolean approved = query.approved();
            entries = entries.stream()
                    .filter(entry -> approved ? entry.approvedAt() != null : entry.approvedAt() == null)
                    .toList();
        }

        List<SortCriterion> sortCriteria = ensureSort(query.sort(), new SortCriterion("name", true));
        Comparator<AgencyEntry> comparator = buildAgencyComparator(sortCriteria);
        List<AgencyEntry> sorted = entries.stream()
                .sorted(comparator)
                .toList();

        return paginate(sorted, query.page(), query.size(), sortCriteria);
    }

    public PagedResult<AgentEntry> listAgents(RolesEnum requesterRole, UUID requesterId, AgentDirectoryQuery query) {
        if (requesterRole == RolesEnum.USER || requesterRole == RolesEnum.AGENT) {
            throw ForbiddenException.actionRequiresRoles(List.of(RolesEnum.ADMIN.name(), RolesEnum.SUPERADMIN.name(), RolesEnum.AGENCY.name()));
        }

        UUID effectiveAgencyId;
        Map<UUID, Agency> agencyMap;
        List<Agent> agents;

        if (requesterRole == RolesEnum.AGENCY) {
            Agency agency = agencyRepository.findByUserId(requesterId)
                    .orElseThrow(() -> {
                        log.warn("Utente {} con ruolo AGENCY senza record agenzia", requesterId);
                        return ForbiddenException.actionRequiresRole(RolesEnum.AGENCY.name());
                    });
            agents = agentRepository.findByAgencyId(agency.id());
            agencyMap = Map.of(agency.id(), agency);
        } else {
            effectiveAgencyId = query.agencyId();
            if (effectiveAgencyId != null) {
                agents = agentRepository.findByAgencyId(effectiveAgencyId);
                agencyMap = agencyRepository.findAll().stream()
                        .filter(agency -> effectiveAgencyId.equals(agency.id()))
                        .collect(Collectors.toMap(Agency::id, agency -> agency));
            } else {
                agents = agentRepository.findAll();
                agencyMap = agencyRepository.findAll().stream()
                        .collect(Collectors.toMap(Agency::id, agency -> agency));
            }
        }

        if (agents.isEmpty()) {
            return new PagedResult<>(List.of(), query.page(), query.size(), 0, sortTokens(query.sort()));
        }

        var userIds = agents.stream().map(Agent::userId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, User> userMap = userIds.isEmpty() ? Map.of() : userRepository.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::id, user -> user));

        List<AgentEntry> entries = getEntries(agents, userMap, agencyMap);

        String search = query.search();
        if (search != null) {
            entries = entries.stream()
                    .filter(entry -> contains(entry.displayName(), search)
                            || contains(entry.email(), search)
                            || contains(entry.reaNumber(), search)
                            || contains(entry.agencyName(), search))
                    .toList();
        }

        List<SortCriterion> sortCriteria = ensureSort(query.sort(), new SortCriterion(DISPLAY_NAME, true));
        Comparator<AgentEntry> comparator = buildAgentComparator(sortCriteria);
        List<AgentEntry> sorted = entries.stream()
                .sorted(comparator)
                .toList();

        return paginate(sorted, query.page(), query.size(), sortCriteria);
    }

    private List<AgentEntry> getEntries(List<Agent> agents, Map<UUID, User> userMap, Map<UUID, Agency> agencyMap) {
        return agents.stream()
                .map(agent -> {
                    var user = userMap.get(agent.userId());
                    var agency = agencyMap.get(agent.agencyId());
                    String photoUrl = resolveMediaUrl(agent.profilePhotoMediaId());
                    return new AgentEntry(
                            agent.id(),
                            agent.userId(),
                            user != null ? Objects.toString(user.displayName(), "") : "",
                            user != null ? Objects.toString(user.email(), "") : "",
                            agent.agencyId(),
                            agency != null ? Objects.toString(agency.name(), "") : "",
                            Objects.toString(agent.reaNumber(), ""),
                            photoUrl,
                            agent.createdAt(),
                            agent.updatedAt()
                    );
                })
                .toList();
    }

    private List<SortCriterion> ensureSort(List<SortCriterion> requested, SortCriterion defaultSort) {
        if (requested == null || requested.isEmpty()) {
            return List.of(defaultSort);
        }
        return requested;
    }

    private Map<UUID, String> resolveRoleCodes() {
        return roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::id, Role::code));
    }

    private String resolveMediaUrl(UUID mediaId) {
        if (mediaId == null) {
            return null;
        }
        return mediaAssetRepository.findById(mediaId)
                .map(MediaAsset::publicUrl)
                .orElseGet(() -> {
                    log.warn("Media asset {} non trovato durante export directory", mediaId);
                    return null;
                });
    }

    private boolean contains(String value, String token) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(token);
    }

    private Comparator<AdminAccount> buildAdminComparator(List<SortCriterion> criteria) {
        List<Comparator<AdminAccount>> comparators = new ArrayList<>();
        for (SortCriterion criterion : criteria) {
            Comparator<AdminAccount> comparator = switch (criterion.property()) {
                case DISPLAY_NAME -> Comparator.comparing(AdminAccount::displayName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "email" -> Comparator.comparing(AdminAccount::email, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "role" -> Comparator.comparing(AdminAccount::role, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "firstAccess" -> Comparator.comparing(AdminAccount::firstAccess);
                case CREATED_AT -> Comparator.comparing(AdminAccount::createdAt, Comparator.nullsLast(OffsetDateTime::compareTo));
                case UPDATED_AT -> Comparator.comparing(AdminAccount::updatedAt, Comparator.nullsLast(OffsetDateTime::compareTo));
                default -> null;
            };
            if (comparator != null) {
                comparators.add(criterion.ascending() ? comparator : comparator.reversed());
            }
        }
        return chainComparators(comparators, Comparator.comparing(AdminAccount::displayName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    }

    private Comparator<AgencyEntry> buildAgencyComparator(List<SortCriterion> criteria) {
        List<Comparator<AgencyEntry>> comparators = new ArrayList<>();
        for (SortCriterion criterion : criteria) {
            Comparator<AgencyEntry> comparator = switch (criterion.property()) {
                case "name" -> Comparator.comparing(AgencyEntry::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "ownerDisplayName" -> Comparator.comparing(AgencyEntry::ownerDisplayName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "ownerEmail" -> Comparator.comparing(AgencyEntry::ownerEmail, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "approvedAt" -> Comparator.comparing(AgencyEntry::approvedAt, Comparator.nullsLast(OffsetDateTime::compareTo));
                case CREATED_AT -> Comparator.comparing(AgencyEntry::createdAt, Comparator.nullsLast(OffsetDateTime::compareTo));
                case UPDATED_AT -> Comparator.comparing(AgencyEntry::updatedAt, Comparator.nullsLast(OffsetDateTime::compareTo));
                default -> null;
            };
            if (comparator != null) {
                comparators.add(criterion.ascending() ? comparator : comparator.reversed());
            }
        }
        return chainComparators(comparators, Comparator.comparing(AgencyEntry::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    }

    private Comparator<AgentEntry> buildAgentComparator(List<SortCriterion> criteria) {
        List<Comparator<AgentEntry>> comparators = new ArrayList<>();
        for (SortCriterion criterion : criteria) {
            Comparator<AgentEntry> comparator = switch (criterion.property()) {
                case DISPLAY_NAME -> Comparator.comparing(AgentEntry::displayName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "email" -> Comparator.comparing(AgentEntry::email, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "reaNumber" -> Comparator.comparing(AgentEntry::reaNumber, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "agencyName" -> Comparator.comparing(AgentEntry::agencyName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case CREATED_AT -> Comparator.comparing(AgentEntry::createdAt, Comparator.nullsLast(OffsetDateTime::compareTo));
                case UPDATED_AT -> Comparator.comparing(AgentEntry::updatedAt, Comparator.nullsLast(OffsetDateTime::compareTo));
                default -> null;
            };
            if (comparator != null) {
                comparators.add(criterion.ascending() ? comparator : comparator.reversed());
            }
        }
        return chainComparators(comparators, Comparator.comparing(AgentEntry::displayName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    }

    private <T> Comparator<T> chainComparators(List<Comparator<T>> comparators, Comparator<T> defaultComparator) {
        Comparator<T> combined = null;
        for (Comparator<T> comparator : comparators) {
            if (comparator == null) {
                continue;
            }
            combined = combined == null ? comparator : combined.thenComparing(comparator);
        }
        return combined != null ? combined : defaultComparator;
    }

    private <T> PagedResult<T> paginate(List<T> sortedItems, int page, int size, List<SortCriterion> sortCriteria) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int totalElements = sortedItems.size();
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);

        List<T> pageItems = sortedItems.subList(fromIndex, toIndex);
        return new PagedResult<>(List.copyOf(pageItems), safePage, safeSize, totalElements, sortTokens(sortCriteria));
    }

    private List<String> sortTokens(List<SortCriterion> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return List.of();
        }
        return criteria.stream()
                .map(c -> c.property() + "," + (c.ascending() ? "ASC" : "DESC"))
                .toList();
    }

    public record SortCriterion(String property, boolean ascending) {}

    public record AdminDirectoryQuery(int page,
                                      int size,
                                      List<SortCriterion> sort,
                                      String search,
                                      RolesEnum roleFilter) {}

    public record AgencyDirectoryQuery(int page,
                                       int size,
                                       List<SortCriterion> sort,
                                       String search,
                                       Boolean approved) {}

    public record AgentDirectoryQuery(int page,
                                      int size,
                                      List<SortCriterion> sort,
                                      String search,
                                      UUID agencyId) {}

    public record PagedResult<T>(List<T> items,
                                 int page,
                                 int size,
                                 int totalElements,
                                 List<String> sort) {
        public int totalPages() {
            if (size <= 0) {
                return 0;
            }
            return (int) Math.ceil((double) totalElements / size);
        }
    }

    public record AdminAccount(UUID id,
                               String displayName,
                               String email,
                               String role,
                               boolean firstAccess,
                               java.time.OffsetDateTime createdAt,
                               java.time.OffsetDateTime updatedAt) {}

    public record AgencyEntry(UUID id,
                              String name,
                              String description,
                              UUID ownerUserId,
                              String ownerDisplayName,
                              String ownerEmail,
                              String logoUrl,
                              UUID approvedBy,
                              java.time.OffsetDateTime approvedAt,
                              java.time.OffsetDateTime createdAt,
                              java.time.OffsetDateTime updatedAt) {}

    public record AgentEntry(UUID id,
                             UUID userId,
                             String displayName,
                             String email,
                             UUID agencyId,
                             String agencyName,
                             String reaNumber,
                             String profilePhotoUrl,
                             java.time.OffsetDateTime createdAt,
                             java.time.OffsetDateTime updatedAt) {}
}
