package it.dieti.dietiestatesbackend.application.user;

import it.dieti.dietiestatesbackend.domain.agency.AgencyRepository;
import it.dieti.dietiestatesbackend.domain.agent.AgentRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileService {
    private final AgencyRepository agencyRepository;
    private final AgentRepository agentRepository;

    public UserProfileService(AgencyRepository agencyRepository, AgentRepository agentRepository) {
        this.agencyRepository = agencyRepository;
        this.agentRepository = agentRepository;
    }

    public Optional<AgencyProfile> findAgencyProfile(UUID userId) {
        return agencyRepository.findByUserId(userId)
                .map(a -> new AgencyProfile(a.name(), a.description(), a.logoMediaId()));
    }

    public Optional<AgentProfile> findAgentProfile(UUID userId) {
        return agentRepository.findByUserId(userId)
                .map(agent -> new AgentProfile(
                        agent.agencyId(),
                        agent.reaNumber(),
                        agent.profilePhotoMediaId()
                ));
    }

    public record AgencyProfile(String name, String description, UUID logoMediaId) {}
    public record AgentProfile(UUID agencyId, String reaNumber, UUID profilePhotoMediaId) {}
}
