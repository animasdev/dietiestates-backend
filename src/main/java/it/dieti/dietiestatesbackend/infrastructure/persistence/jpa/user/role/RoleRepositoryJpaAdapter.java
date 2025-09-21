package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.role;

import it.dieti.dietiestatesbackend.domain.user.role.Role;
import it.dieti.dietiestatesbackend.domain.user.role.RoleRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RoleRepositoryJpaAdapter implements RoleRepository {
    private final RoleJpaRepository jpaRepository;

    public RoleRepositoryJpaAdapter(RoleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Role> findAll() {
        return jpaRepository.findAll(Sort.by("code").ascending()).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Role> findByCode( String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }

    private Role toDomain(RoleEntity roleEntity) {
        return new Role(
                roleEntity.getId(),
                roleEntity.getCode(),
                roleEntity.getName(),
                roleEntity.getDescription());
    }
}
