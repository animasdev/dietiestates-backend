package it.dieti.dietiestatesbackend.application.user;

import it.dieti.dietiestatesbackend.domain.agency.AgencyRepository;
import it.dieti.dietiestatesbackend.domain.agent.AgentRepository;
import it.dieti.dietiestatesbackend.domain.media.MediaAsset;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileService {
    private final AgencyRepository agencyRepository;
    private final AgentRepository agentRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    public UserProfileService(AgencyRepository agencyRepository,
                              AgentRepository agentRepository,
                              MediaAssetRepository mediaAssetRepository) {
        this.agencyRepository = agencyRepository;
        this.agentRepository = agentRepository;
        this.mediaAssetRepository = mediaAssetRepository;
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
