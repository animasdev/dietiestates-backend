package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.AdminSummary;
import it.dieti.dietiestatesbackend.api.model.AdminSummaryPage;
import it.dieti.dietiestatesbackend.api.model.AgencySummary;
import it.dieti.dietiestatesbackend.api.model.AgencySummaryPage;
import it.dieti.dietiestatesbackend.api.model.AgentSummary;
import it.dieti.dietiestatesbackend.api.model.AgentSummaryPage;
import it.dieti.dietiestatesbackend.api.model.PageMetadata;
import it.dieti.dietiestatesbackend.api.model.UserInfo;
import it.dieti.dietiestatesbackend.api.model.UserInfoAgencyProfile;
import it.dieti.dietiestatesbackend.api.model.UserInfoAgentProfile;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.application.user.UserDirectoryService;
import it.dieti.dietiestatesbackend.application.user.UserProfileService;
import it.dieti.dietiestatesbackend.application.user.UserProfileService.AgentProfile;
import it.dieti.dietiestatesbackend.application.user.UserProfileService.AgencyProfile;
import it.dieti.dietiestatesbackend.application.user.UserService;
import it.dieti.dietiestatesbackend.domain.user.role.RolesEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class UsersApiDelegateImpl implements UsersApiDelegate {
    public static final String UPDATED_AT = "updatedAt";
    public static final String CREATED_AT = "createdAt";
    private final UserService userService;
    private final UserProfileService userProfileService;
    private final UserDirectoryService userDirectoryService;
    private static final Logger log = LoggerFactory.getLogger(UsersApiDelegateImpl.class);

    public UsersApiDelegateImpl(UserService userService,
                                UserProfileService userProfileService,
                                UserDirectoryService userDirectoryService) {
        this.userService = userService;
        this.userProfileService = userProfileService;
        this.userDirectoryService = userDirectoryService;
    }

    @Override
    public ResponseEntity<UserInfo> usersMeGet() {
        var ctx = requireAuthenticatedUser();
        var user = ctx.user();

        var body = new UserInfo();
        body.setDisplayName(user.displayName());
        body.setEmail(user.email());
        body.setRole(ctx.roleCode());

        userProfileService.findAgencyProfile(user.id())
                .ifPresent(profile -> body.setAgencyProfile(toApi(profile)));

        userProfileService.findAgentProfile(user.id())
                .ifPresent(profile -> body.setAgentProfile(toApi(profile)));

        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<AdminSummaryPage> usersAdminsGet(Integer page, Integer size, List<String> sort, String search, String role) {
        var ctx = requireAuthenticatedUser();
        ensureRole(ctx.role(), RolesEnum.SUPERADMIN, RolesEnum.ADMIN);

        var query = new UserDirectoryService.AdminDirectoryQuery(
                normalizePage(page),
                normalizeSize(size),
                parseSort(sort, SortTarget.ADMINS),
                normalizeSearch(search),
                parseAdminRole(role)
        );

        var result = userDirectoryService.listAdminAccounts(query);
        return ResponseEntity.ok(toAdminPage(result));
    }

    @Override
    public ResponseEntity<AgencySummaryPage> usersAgenciesGet(Integer page, Integer size, List<String> sort, String search, Boolean approved) {
        var ctx = requireAuthenticatedUser();
        ensureRole(ctx.role(), RolesEnum.SUPERADMIN, RolesEnum.ADMIN);

        var query = new UserDirectoryService.AgencyDirectoryQuery(
                normalizePage(page),
                normalizeSize(size),
                parseSort(sort, SortTarget.AGENCIES),
                normalizeSearch(search),
                approved
        );

        var result = userDirectoryService.listAgencies(query);
        return ResponseEntity.ok(toAgencyPage(result));
    }

    @Override
    public ResponseEntity<AgentSummaryPage> usersAgentsGet(Integer page, Integer size, List<String> sort, String search, UUID agencyId) {
        var ctx = requireAuthenticatedUser();
        ensureRole(ctx.role(), RolesEnum.SUPERADMIN, RolesEnum.ADMIN, RolesEnum.AGENCY);

        var query = new UserDirectoryService.AgentDirectoryQuery(
                normalizePage(page),
                normalizeSize(size),
                parseSort(sort, SortTarget.AGENTS),
                normalizeSearch(search),
                agencyId
        );

        var result = userDirectoryService.listAgents(ctx.role(), ctx.user().id(), query);
        return ResponseEntity.ok(toAgentPage(result));
    }

    private it.dieti.dietiestatesbackend.domain.user.User resolveUser(UUID userId) {
        try {
            return userService.findUserById(userId);
        } catch (NoSuchElementException ex) {
            log.warn("Utente {} non trovato durante users/me", userId);
            throw UnauthorizedException.userNotFound();
        }
    }

    private AuthenticatedUserContext requireAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)){
            log.warn("Accesso protetto senza token");
            throw UnauthorizedException.bearerTokenMissing();
        }

        var token = jwtAuth.getToken();
        var userId = UUID.fromString(token.getSubject());
        var user = resolveUser(userId);
        var roleCode = resolveRoleCode(user.roleId(), userId);
        RolesEnum roleEnum;
        try {
            roleEnum = RolesEnum.valueOf(roleCode);
        } catch (IllegalArgumentException ex) {
            log.error("Ruolo {} non riconosciuto per utente {}", roleCode, userId);
            throw UnauthorizedException.userNotFound();
        }
        return new AuthenticatedUserContext(user, roleEnum, roleCode);
    }

    private String resolveRoleCode(UUID roleId, UUID userId) {
        try {
            return userService.getRoleCode(roleId);
        } catch (NoSuchElementException ex) {
            log.warn("Ruolo {} non trovato per utente {}", roleId, userId);
            throw UnauthorizedException.userNotFound();
        }
    }

    private void ensureRole(RolesEnum actual, RolesEnum... allowed) {
        for (RolesEnum value : allowed) {
            if (actual == value) {
                return;
            }
        }
        var required = Arrays.stream(allowed).map(RolesEnum::name).toList();
        throw ForbiddenException.actionRequiresRoles(required);
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw BadRequestException.forField("page", "Il numero pagina deve essere maggiore o uguale a 0.");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return 20;
        }
        if (size < 1 || size > 100) {
            throw BadRequestException.forField("size", "La dimensione della pagina deve essere tra 1 e 100.");
        }
        return size;
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        var normalized = search.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private RolesEnum parseAdminRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            var parsed = RolesEnum.valueOf(role.trim().toUpperCase(Locale.ROOT));
            if (parsed != RolesEnum.ADMIN && parsed != RolesEnum.SUPERADMIN) {
                throw BadRequestException.forField("role", "Valori ammessi: SUPERADMIN, ADMIN.");
            }
            return parsed;
        } catch (IllegalArgumentException ex) {
            throw BadRequestException.forField("role", "Valori ammessi: SUPERADMIN, ADMIN.");
        }
    }

    private List<UserDirectoryService.SortCriterion> parseSort(List<String> sortValues, SortTarget target) {
        if (sortValues == null || sortValues.isEmpty()) {
            return List.of();
        }
        List<UserDirectoryService.SortCriterion> criteria = new ArrayList<>();
        for (String value : sortValues) {
            if (value == null || value.isBlank()) {
                continue;
            }
            var parts = value.split(",", 2);
            var property = parts[0].trim();
            if (!isAllowedProperty(property, target)) {
                continue;
            }
            boolean ascending = parts.length < 2 || !"desc".equalsIgnoreCase(parts[1].trim());
            criteria.add(new UserDirectoryService.SortCriterion(property, ascending));
        }
        return criteria;
    }

    private boolean isAllowedProperty(String property, SortTarget target) {
        return switch (target) {
            case ADMINS -> Set.of("displayName", "email", "role", "firstAccess", CREATED_AT, UPDATED_AT).contains(property);
            case AGENCIES -> Set.of("name", "ownerDisplayName", "ownerEmail", "approvedAt", CREATED_AT, UPDATED_AT).contains(property);
            case AGENTS -> Set.of("displayName", "email", "reaNumber", "agencyName", CREATED_AT, UPDATED_AT).contains(property);
        };
    }

    private UserInfoAgencyProfile toApi(AgencyProfile profile) {
        UserInfoAgencyProfile api = new UserInfoAgencyProfile();
        api.setName(profile.name());
        api.setDescription(profile.description());
        if (profile.logoUrl() != null && !profile.logoUrl().isBlank()) {
            api.setLogoUrl(URI.create(profile.logoUrl()));
        }
        return api;
    }

    private UserInfoAgentProfile toApi(AgentProfile profile) {
        UserInfoAgentProfile api = new UserInfoAgentProfile();
        api.setAgencyId(profile.agencyId());
        api.setReaNumber(profile.reaNumber());
        if (profile.profilePhotoUrl() != null && !profile.profilePhotoUrl().isBlank()) {
            api.setProfilePhotoUrl(URI.create(profile.profilePhotoUrl()));
        }
        return api;
    }

    private AdminSummary toApi(UserDirectoryService.AdminAccount account) {
        AdminSummary summary = new AdminSummary();
        summary.setId(account.id());
        summary.setDisplayName(account.displayName());
        summary.setEmail(account.email());
        summary.setRole(account.role());
        summary.setFirstAccess(account.firstAccess());
        summary.setCreatedAt(account.createdAt());
        summary.setUpdatedAt(account.updatedAt());
        return summary;
    }

    private AgencySummary toApi(UserDirectoryService.AgencyEntry entry) {
        AgencySummary summary = new AgencySummary();
        summary.setId(entry.id());
        summary.setName(entry.name());
        summary.setDescription(entry.description());
        summary.setOwnerUserId(entry.ownerUserId());
        summary.setOwnerDisplayName(entry.ownerDisplayName());
        summary.setOwnerEmail(entry.ownerEmail());
        var logo = toUri(entry.logoUrl());
        if (logo != null) {
            summary.setLogoUrl(logo);
        }
        summary.setApprovedBy(entry.approvedBy());
        summary.setApprovedAt(entry.approvedAt());
        summary.setCreatedAt(entry.createdAt());
        summary.setUpdatedAt(entry.updatedAt());
        return summary;
    }

    private AgentSummary toApi(UserDirectoryService.AgentEntry entry) {
        AgentSummary summary = new AgentSummary();
        summary.setId(entry.id());
        summary.setUserId(entry.userId());
        summary.setDisplayName(entry.displayName());
        summary.setEmail(entry.email());
        summary.setAgencyId(entry.agencyId());
        summary.setAgencyName(entry.agencyName());
        summary.setReaNumber(entry.reaNumber());
        var photo = toUri(entry.profilePhotoUrl());
        if (photo != null) {
            summary.setProfilePhotoUrl(photo);
        }
        summary.setCreatedAt(entry.createdAt());
        summary.setUpdatedAt(entry.updatedAt());
        return summary;
    }

    private AdminSummaryPage toAdminPage(UserDirectoryService.PagedResult<UserDirectoryService.AdminAccount> result) {
        AdminSummaryPage page = new AdminSummaryPage();
        page.setItems(result.items().stream().map(this::toApi).toList());
        page.setPage(toMetadata(result));
        return page;
    }

    private AgencySummaryPage toAgencyPage(UserDirectoryService.PagedResult<UserDirectoryService.AgencyEntry> result) {
        AgencySummaryPage page = new AgencySummaryPage();
        page.setItems(result.items().stream().map(this::toApi).toList());
        page.setPage(toMetadata(result));
        return page;
    }

    private AgentSummaryPage toAgentPage(UserDirectoryService.PagedResult<UserDirectoryService.AgentEntry> result) {
        AgentSummaryPage page = new AgentSummaryPage();
        page.setItems(result.items().stream().map(this::toApi).toList());
        page.setPage(toMetadata(result));
        return page;
    }

    private PageMetadata toMetadata(UserDirectoryService.PagedResult<?> result) {
        PageMetadata metadata = new PageMetadata();
        metadata.setPage(result.page());
        metadata.setSize(result.size());
        metadata.setTotalElements(result.totalElements());
        metadata.setTotalPages(result.totalPages());
        metadata.setSort(result.sort());
        return metadata;
    }

    private enum SortTarget {
        ADMINS,
        AGENCIES,
        AGENTS
    }

    private URI toUri(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return URI.create(value);
    }

    private record AuthenticatedUserContext(it.dieti.dietiestatesbackend.domain.user.User user,
                                            RolesEnum role,
                                            String roleCode) {}
}
