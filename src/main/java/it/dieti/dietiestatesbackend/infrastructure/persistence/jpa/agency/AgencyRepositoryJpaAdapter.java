package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.agency;

import it.dieti.dietiestatesbackend.domain.agency.Agency;
import it.dieti.dietiestatesbackend.domain.agency.AgencyRepository;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media.MediaAssetEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.UserEntity;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AgencyRepositoryJpaAdapter implements AgencyRepository {
    private final AgencyJpaRepository jpaRepository;

    public AgencyRepositoryJpaAdapter(AgencyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Agency> findAll() {
        return jpaRepository.findAll(Sort.by("name").ascending())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Agency> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Agency> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public Agency save(Agency agency) {
        AgencyEntity entity;
        if (agency.id() != null) {
            entity = jpaRepository.findById(agency.id())
                    .orElseThrow(() -> new IllegalArgumentException("Agency not found: " + agency.id()));
        } else {
            entity = new AgencyEntity();
            entity.setCreatedAt(OffsetDateTime.now());
        }

        entity.setName(agency.name());
        entity.setDescription(agency.description());

        var userRef = new UserEntity();
        userRef.setId(agency.userId());
        entity.setUser(userRef);

        if (agency.logoMediaId() != null) {
            var logoRef = new MediaAssetEntity();
            logoRef.setId(agency.logoMediaId());
            entity.setLogoMedia(logoRef);
        } else {
            entity.setLogoMedia(null);
        }

        if (agency.approvedBy() != null) {
            var approvedByRef = new UserEntity();
            approvedByRef.setId(agency.approvedBy());
            entity.setApprovedBy(approvedByRef);
            entity.setApprovedAt(agency.approvedAt());
        } else {
            entity.setApprovedBy(null);
            entity.setApprovedAt(null);
        }

        entity.setUpdatedAt(OffsetDateTime.now());

        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private Agency toDomain(AgencyEntity entity) {
        return new Agency(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getLogoMedia() != null ? entity.getLogoMedia().getId() : null,
                entity.getApprovedBy() != null ? entity.getApprovedBy().getId() : null,
                entity.getApprovedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
