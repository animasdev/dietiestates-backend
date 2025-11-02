package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.agent;

import it.dieti.dietiestatesbackend.domain.agent.Agent;
import it.dieti.dietiestatesbackend.domain.agent.AgentRepository;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.agency.AgencyEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media.MediaAssetEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.UserEntity;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AgentRepositoryJpaAdapter implements AgentRepository {
    private final AgentJpaRepository jpaRepository;

    public AgentRepositoryJpaAdapter(AgentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Agent> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Agent> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Agent> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public List<Agent> findByAgencyId(UUID agencyId) {
        return jpaRepository.findByAgencyId(agencyId).stream().map(this::toDomain).toList();
    }

    @Override
    public Agent save(Agent agent) {
        AgentEntity entity;
        if (agent.id() != null) {
            entity = jpaRepository.findById(agent.id())
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agent.id()));
        } else {
            entity = new AgentEntity();
            entity.setCreatedAt(OffsetDateTime.now());
        }

        var userRef = new UserEntity();
        userRef.setId(agent.userId());
        entity.setUser(userRef);

        var agencyRef = new AgencyEntity();
        agencyRef.setId(agent.agencyId());
        entity.setAgency(agencyRef);

        entity.setReaNumber(agent.reaNumber());

        if (agent.profilePhotoMediaId() != null) {
            var photoRef = new MediaAssetEntity();
            photoRef.setId(agent.profilePhotoMediaId());
            entity.setProfilePhotoMedia(photoRef);
        } else {
            entity.setProfilePhotoMedia(null);
        }

        entity.setUpdatedAt(OffsetDateTime.now());

        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private Agent toDomain(AgentEntity entity) {
        return new Agent(
                entity.getId(),
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getAgency() != null ? entity.getAgency().getId() : null,
                entity.getReaNumber(),
                entity.getProfilePhotoMedia() != null ? entity.getProfilePhotoMedia().getId() : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
