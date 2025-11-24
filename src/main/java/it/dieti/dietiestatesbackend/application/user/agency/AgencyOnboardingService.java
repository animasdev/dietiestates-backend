package it.dieti.dietiestatesbackend.application.user.agency;

import it.dieti.dietiestatesbackend.application.exception.ApplicationHttpException;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.OnboardingException;
import it.dieti.dietiestatesbackend.domain.user.agency.Agency;
import it.dieti.dietiestatesbackend.domain.user.agency.AgencyRepository;
import it.dieti.dietiestatesbackend.domain.user.User;
import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RoleRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RolesEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AgencyOnboardingService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AgencyRepository agencyRepository;
    private static final Logger log = LoggerFactory.getLogger(AgencyOnboardingService.class);

    public AgencyOnboardingService(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   AgencyRepository agencyRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.agencyRepository = agencyRepository;
    }

    public record CompleteAgencyProfileCommand(String name, String description, UUID logoMediaId) {}

    @Transactional
    public Agency completeProfile(UUID userId, CompleteAgencyProfileCommand command) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(command, "command is required");

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new OnboardingException(OnboardingException.Reason.USER_NOT_FOUND, "User not found"));
        ensureFirstAccessPending(user);

        var role = roleRepository.findById(user.roleId())
                .orElseThrow(() -> new OnboardingException(OnboardingException.Reason.ROLE_NOT_ALLOWED, "Role not found"));
        var roleEnum = toRoleEnum(role.code());
        if (roleEnum != RolesEnum.AGENCY) {
            throw new OnboardingException(OnboardingException.Reason.ROLE_NOT_ALLOWED, "User role does not allow agency onboarding");
        }

        if (agencyRepository.findByUserId(userId).isPresent()) {
            throw new OnboardingException(OnboardingException.Reason.PROFILE_ALREADY_EXISTS, "Agency profile already exists");
        }

        var name = normalize(command.name());
        var description = normalize(command.description());

        List<ApplicationHttpException.FieldErrorDetail> fieldErrors = new ArrayList<>();
        if (name.isBlank()) {
            fieldErrors.add(new ApplicationHttpException.FieldErrorDetail("name", "Il campo 'name' è obbligatorio."));
        }
        if (description.isBlank()) {
            fieldErrors.add(new ApplicationHttpException.FieldErrorDetail("description", "Il campo 'description' è obbligatorio."));
        }
        if (!fieldErrors.isEmpty()) {
            log.warn("Completamento profilo agenzia non valido per user {}: campi mancanti", userId);
            throw BadRequestException.forFields("Richiesta non valida: completare tutti i campi obbligatori.", fieldErrors);
        }

        var agency = new Agency(
                null,
                name,
                description,
                userId,
                command.logoMediaId(),
                null,
                null,
                null,
                null
        );
        var saved = agencyRepository.save(agency);
        markFirstAccessCompleted(user);
        return saved;
    }

    private void ensureFirstAccessPending(User user) {
        if (!user.firstAccess()) {
            throw new OnboardingException(OnboardingException.Reason.FIRST_ACCESS_ALREADY_COMPLETED, "First access already completed");
        }
    }

    private void markFirstAccessCompleted(User user) {
        var updated = new User(
                user.id(),
                user.displayName(),
                user.email(),
                false,
                user.roleId(),
                user.passwordHash(),
                user.passwordAlgo(),
                user.createdAt(),
                user.updatedAt(),
                user.invitedByUserId()
        );
        userRepository.update(updated);
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim();
    }

    private RolesEnum toRoleEnum(String code) {
        try {
            return RolesEnum.valueOf(code);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalStateException("Unsupported role code: " + code, ex);
        }
    }
}
