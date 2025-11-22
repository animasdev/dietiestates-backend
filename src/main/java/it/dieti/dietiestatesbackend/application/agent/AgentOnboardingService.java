package it.dieti.dietiestatesbackend.application.agent;

import it.dieti.dietiestatesbackend.application.exception.ApplicationHttpException;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.OnboardingException;
import it.dieti.dietiestatesbackend.domain.agent.Agent;
import it.dieti.dietiestatesbackend.domain.agent.AgentRepository;
import it.dieti.dietiestatesbackend.domain.agency.AgencyRepository;
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
public class AgentOnboardingService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AgentRepository agentRepository;
    private final AgencyRepository agencyRepository;
    private static final Logger log = LoggerFactory.getLogger(AgentOnboardingService.class);

    public AgentOnboardingService(UserRepository userRepository,
                                  RoleRepository roleRepository,
                                  AgentRepository agentRepository,
                                  AgencyRepository agencyRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.agentRepository = agentRepository;
        this.agencyRepository = agencyRepository;
    }

    public record CompleteAgentProfileCommand(UUID agencyId, String reaNumber, UUID profilePhotoMediaId) {}

    @Transactional
    public Agent completeProfile(UUID userId, CompleteAgentProfileCommand command) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(command, "command is required");

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new OnboardingException(OnboardingException.Reason.USER_NOT_FOUND, "User not found"));
        ensureFirstAccessPending(user);

        var role = roleRepository.findById(user.roleId())
                .orElseThrow(() -> new OnboardingException(OnboardingException.Reason.ROLE_NOT_ALLOWED, "Role not found"));
        var roleEnum = toRoleEnum(role.code());
        if (roleEnum != RolesEnum.AGENT) {
            throw new OnboardingException(OnboardingException.Reason.ROLE_NOT_ALLOWED, "User role does not allow agent onboarding");
        }

        agentRepository.findByUserId(userId)
                .ifPresent(a -> { throw new OnboardingException(OnboardingException.Reason.PROFILE_ALREADY_EXISTS, "Agent profile already exists"); });

        var agencyId = command.agencyId();
        if (agencyId == null) {
            log.warn("Completamento profilo agente non valido per user {}: agencyId mancante", userId);
            throw BadRequestException.forField("agencyId", "Il campo 'agencyId' è obbligatorio.");
        }
        if (agencyRepository.findById(agencyId).isEmpty()) {
            throw new OnboardingException(OnboardingException.Reason.AGENCY_NOT_FOUND, "Agency not found");
        }

        var reaNumber = normalize(command.reaNumber());
        if (reaNumber.isBlank()) {
            List<ApplicationHttpException.FieldErrorDetail> errors = new ArrayList<>();
            errors.add(new ApplicationHttpException.FieldErrorDetail("reaNumber", "Il campo 'reaNumber' è obbligatorio."));
            log.warn("Completamento profilo agente non valido per user {}: reaNumber mancante", userId);
            throw BadRequestException.forFields("Richiesta non valida: completare tutti i campi obbligatori.", errors);
        }

        var agent = new Agent(
                null,
                userId,
                agencyId,
                reaNumber,
                command.profilePhotoMediaId(),
                null,
                null
        );
        var saved = agentRepository.save(agent);
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
