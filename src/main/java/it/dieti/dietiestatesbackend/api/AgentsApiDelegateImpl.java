package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.Agent;
import it.dieti.dietiestatesbackend.api.model.AgentCreateRequest;
import it.dieti.dietiestatesbackend.application.agent.AgentOnboardingService;
import it.dieti.dietiestatesbackend.application.onboarding.OnboardingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AgentsApiDelegateImpl implements AgentsApiDelegate {
    private final AgentOnboardingService agentOnboardingService;

    public AgentsApiDelegateImpl(AgentOnboardingService agentOnboardingService) {
        this.agentOnboardingService = agentOnboardingService;
    }

    @Override
    public ResponseEntity<Agent> agentsPost(AgentCreateRequest agentCreateRequest) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var userId = UUID.fromString(jwtAuth.getToken().getSubject());
        try {
            var command = new AgentOnboardingService.CompleteAgentProfileCommand(
                    agentCreateRequest.getAgencyId(),
                    agentCreateRequest.getReaNumber(),
                    agentCreateRequest.getProfilePhotoMediaId()
            );
            var agent = agentOnboardingService.completeProfile(userId, command);
            return ResponseEntity.status(HttpStatus.CREATED).body(toApi(agent));
        } catch (OnboardingException ex) {
            return ResponseEntity.status(mapStatus(ex.reason())).build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    private HttpStatus mapStatus(OnboardingException.Reason reason) {
        return switch (reason) {
            case USER_NOT_FOUND -> HttpStatus.UNAUTHORIZED;
            case ROLE_NOT_ALLOWED -> HttpStatus.FORBIDDEN;
            case PROFILE_ALREADY_EXISTS, FIRST_ACCESS_ALREADY_COMPLETED -> HttpStatus.CONFLICT;
            case AGENCY_NOT_FOUND -> HttpStatus.BAD_REQUEST;
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
