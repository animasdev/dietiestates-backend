package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.Agent;
import it.dieti.dietiestatesbackend.api.model.AgentCreateRequest;
import it.dieti.dietiestatesbackend.application.agent.AgentOnboardingService;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.ConflictException;
import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.application.onboarding.OnboardingException;
import it.dieti.dietiestatesbackend.domain.agency.AgencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static it.dieti.dietiestatesbackend.application.onboarding.OnboardingException.Reason.AGENCY_NOT_FOUND;

@Service
public class AgentsApiDelegateImpl implements AgentsApiDelegate {
    private final AgentOnboardingService agentOnboardingService;
    private static final Logger log = LoggerFactory.getLogger(AgentsApiDelegateImpl.class);
    private final AgencyRepository agencyRepository;

    public AgentsApiDelegateImpl(AgentOnboardingService agentOnboardingService, AgencyRepository agencyRepository) {
        this.agentOnboardingService = agentOnboardingService;
        this.agencyRepository = agencyRepository;
    }

    @Override
    public ResponseEntity<Agent> agentsPost(AgentCreateRequest agentCreateRequest) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Tentativo di completare profilo agente senza token");
            throw UnauthorizedException.bearerTokenMissing();
        }

        var userId = UUID.fromString(jwtAuth.getToken().getSubject());
        try {
            var agency = agencyRepository.findByUserId(agentCreateRequest.getAgencyId()).orElseThrow(()->new OnboardingException(AGENCY_NOT_FOUND,""));
            log.info("[Agent Onboarding] agenzia torvata? {}",agency.id());
            var command = new AgentOnboardingService.CompleteAgentProfileCommand(
                    agency.id(),
                    agentCreateRequest.getReaNumber(),
                    agentCreateRequest.getProfilePhotoMediaId()
            );
            var agent = agentOnboardingService.completeProfile(userId, command);
            return ResponseEntity.status(HttpStatus.CREATED).body(toApi(agent));
        } catch (OnboardingException ex) {
            throw translateOnboardingException(ex, userId);
        }
    }

    private RuntimeException translateOnboardingException(OnboardingException ex, UUID userId) {
        return switch (ex.reason()) {
            case USER_NOT_FOUND -> {
                log.warn("Utente {} non trovato durante onboarding agente", userId);
                yield UnauthorizedException.userNotFound();
            }
            case ROLE_NOT_ALLOWED -> {
                log.warn("Utente {} senza ruolo valido per onboarding agente", userId);
                yield ForbiddenException.actionRequiresRole("AGENT");
            }
            case PROFILE_ALREADY_EXISTS -> {
                log.warn("Utente {} ha già un profilo agente", userId);
                yield ConflictException.of("Profilo agente già completato.");
            }
            case FIRST_ACCESS_ALREADY_COMPLETED -> {
                log.warn("Utente {} ha già completato il firstAccess per profilo agente", userId);
                yield ConflictException.of("Profilo agente già confermato.");
            }
            case AGENCY_NOT_FOUND -> {
                log.warn("Agenzia non trovata durante onboarding agente per user {}", userId);
                yield BadRequestException.of("Agenzia indicata non trovata.");
            }
        };
    }

    private Agent toApi(it.dieti.dietiestatesbackend.domain.agent.Agent agent) {
        Agent body = new Agent();
        body.setId(agent.id());
        body.setUserId(agent.userId());
        body.setAgencyId(agent.agencyId());
        body.setReaNumber(agent.reaNumber());
        body.setProfilePhotoMediaId(agent.profilePhotoMediaId());
        body.setCreatedAt(agent.createdAt());
        body.setUpdatedAt(agent.updatedAt());
        return body;
    }
}
