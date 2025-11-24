package it.dieti.dietiestatesbackend.domain.user.agent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentRepository {
    List<Agent> findAll();
    Optional<Agent> findById(UUID id);
    Optional<Agent> findByUserId(UUID userId);
    List<Agent> findByAgencyId(UUID agencyId);
    Agent save(Agent agent);
    void deleteById(UUID id);
}
