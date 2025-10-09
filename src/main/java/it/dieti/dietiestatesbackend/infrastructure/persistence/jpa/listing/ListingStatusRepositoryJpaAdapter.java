package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing;

import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatus;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ListingStatusRepositoryJpaAdapter implements ListingStatusRepository {
    private final ListingStatusJpaRepository jpaRepository;

    public ListingStatusRepositoryJpaAdapter(ListingStatusJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ListingStatus> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }

    private ListingStatus toDomain(ListingStatusEntity entity) {
        return new ListingStatus(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getSortOrder(),
                entity.getCreatedAt()
        );
    }
}
