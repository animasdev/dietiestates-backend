package it.dieti.dietiestatesbackend.application.user;

import it.dieti.dietiestatesbackend.application.exception.NotFoundException;
import it.dieti.dietiestatesbackend.domain.user.agency.AgencyRepository;
import it.dieti.dietiestatesbackend.domain.user.agent.AgentRepository;
import it.dieti.dietiestatesbackend.domain.media.MediaAsset;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetRepository;
import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileService {
    private final AgencyRepository agencyRepository;
    private final AgentRepository agentRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    public UserProfileService(AgencyRepository agencyRepository,
                              AgentRepository agentRepository,
                              MediaAssetRepository mediaAssetRepository, UserRepository userRepository) {
        this.agencyRepository = agencyRepository;
        this.agentRepository = agentRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.userRepository = userRepository;
    }

    public Optional<AgencyProfile> findAgencyProfile(UUID userId) {
        return agencyRepository.findByUserId(userId)
                .map(a -> new AgencyProfile(a.id(), a.name(), a.description(), resolveMediaUrl(a.logoMediaId())));
    }

    public Optional<AgentProfile> findAgentProfile(UUID userId) {
        return agentRepository.findByUserId(userId)
                .map(agent -> new AgentProfile(
                        agent.agencyId(),
                        agent.id(),
                        agent.reaNumber(),
                        resolveMediaUrl(agent.profilePhotoMediaId())
                ));
    }

    public AgentPublicInfo getAgentInfo(UUID agentId) {
        var agent = agentRepository.findById(agentId)
                .orElseThrow(() -> NotFoundException.resourceNotFound("Agente", agentId));
        var user = userRepository.findById(agent.userId())
                .orElseThrow(() -> {
                    log.warn("Utente {} non trovato per agente {}", agent.userId(), agentId);
                    return NotFoundException.resourceNotFound("Agente", agentId);
                });
        var photoUrl = resolveMediaUrl(agent.profilePhotoMediaId());
        return new AgentPublicInfo(
                Objects.toString(user.displayName(), ""),
                Objects.toString(user.email(), ""),
                photoUrl
        );
    }

    public AgencyPublicInfo getAgencyInfo(UUID agencyId) {
        var agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> NotFoundException.resourceNotFound("Agenzia", agencyId));
        var owner = userRepository.findById(agency.userId())
                .orElse(null);
        var logoUrl = resolveMediaUrl(agency.logoMediaId());
        return new AgencyPublicInfo(
                Objects.toString(agency.name(), ""),
                owner != null ? Objects.toString(owner.email(), "") : "",
                logoUrl
        );
    }


    public record AgentPublicInfo(String displayName, String email, String profilePhotoUrl) {}
    public record AgencyPublicInfo(String name, String email, String logoUrl) {}

    private String resolveMediaUrl(UUID mediaId) {
        if (mediaId == null) {
            return null;
        }
        return mediaAssetRepository.findById(mediaId)
                .map(MediaAsset::publicUrl)
                .orElseGet(() -> {
                    log.warn("Media asset {} non trovato durante composizione profilo utente", mediaId);
                    return null;
                });
    }

    public record AgencyProfile(UUID agencyId, String name, String description, String logoUrl) {}
    public record AgentProfile(UUID agencyId, UUID agentId, String reaNumber, String profilePhotoUrl) {}
}
