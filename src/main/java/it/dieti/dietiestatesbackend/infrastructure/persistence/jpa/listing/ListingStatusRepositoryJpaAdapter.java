package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing;

import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatus;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ListingStatusRepositoryJpaAdapter implements ListingStatusRepository {
    private static final Logger log = LoggerFactory.getLogger(ListingStatusRepositoryJpaAdapter.class);
    private final ListingStatusJpaRepository jpaRepository;

    public ListingStatusRepositoryJpaAdapter(ListingStatusJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ListingStatus> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }

    @Override
    public Optional<ListingStatus> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private ListingStatus toDomain(ListingStatusEntity entity) {
        log.trace("toDomain({})", entity);
        return new ListingStatus(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getSortOrder(),
                entity.getCreatedAt()
        );
    }
}
