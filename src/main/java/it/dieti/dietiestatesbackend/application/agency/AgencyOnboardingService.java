package it.dieti.dietiestatesbackend.application.agency;

import it.dieti.dietiestatesbackend.application.onboarding.OnboardingException;
import it.dieti.dietiestatesbackend.domain.agency.Agency;
import it.dieti.dietiestatesbackend.domain.agency.AgencyRepository;
import it.dieti.dietiestatesbackend.domain.user.User;
import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RoleRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RolesEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class AgencyOnboardingService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AgencyRepository agencyRepository;

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
        if (name.isBlank() || description.isBlank()) {
            throw new IllegalArgumentException("Name and description are required");
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
                user.updatedAt()
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
