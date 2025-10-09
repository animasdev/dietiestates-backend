package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing;

import it.dieti.dietiestatesbackend.domain.listing.ListingType;
import it.dieti.dietiestatesbackend.domain.listing.ListingTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ListingTypeRepositoryJpaAdapter implements ListingTypeRepository {
    private final ListingTypeJpaRepository jpaRepository;

    public ListingTypeRepositoryJpaAdapter(ListingTypeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ListingType> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }

    private ListingType toDomain(ListingTypeEntity entity) {
        return new ListingType(entity.getId(), entity.getCode(), entity.getName());
    }
}
