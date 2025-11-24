package it.dieti.dietiestatesbackend.domain.user.agency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgencyRepository {
    List<Agency> findAll();
    Optional<Agency> findById(UUID id);
    Optional<Agency> findByUserId(UUID userId);
    Agency save(Agency agency);
    void deleteById(UUID id);
}
