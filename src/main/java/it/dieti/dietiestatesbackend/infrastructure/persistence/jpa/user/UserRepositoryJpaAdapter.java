package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user;

import it.dieti.dietiestatesbackend.domain.user.User;
import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.role.RoleEntity;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryJpaAdapter implements UserRepository {
    private final UserJpaRepository jpaRepository;

    public UserRepositoryJpaAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<User> findAll(Pageable pageable) {
        var page = jpaRepository.findAll(pageable);
        return page.getContent().stream().map(this::toDomain).toList();
    }

    private User toDomain(UserEntity e) {
        return new User(
                e.getId(),
                e.getDisplayName(),
                e.getEmail(),
                e.getFirstAccess(),
                e.getRole().getId(),
                e.getPasswordHash(),
                e.getPasswordAlgo(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public User insert(User user) {
        var entity = toEntity(user);
        // Note: id is intentionally left null to let the DB generate it (DEFAULT gen_random_uuid())
        var saved = jpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    private UserEntity toEntity(User user) {
        var entity = new UserEntity();
        var roleRef = new RoleEntity();
        roleRef.setId(user.roleId());

        entity.setEmail(user.email());
        entity.setDisplayName(user.displayName());
        entity.setFirstAccess(user.firstAccess());
        entity.setPasswordHash(user.passwordHash());
        entity.setPasswordAlgo(user.passwordAlgo());
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        entity.setRole(roleRef);
        return entity;
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public User update(User user) {
        var existing = jpaRepository.findById(user.id())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + user.id()));

        existing.setDisplayName(user.displayName());
        // Email changes are currently not allowed; keep existing email
        existing.setFirstAccess(user.firstAccess());
        existing.setPasswordHash(user.passwordHash());
        existing.setPasswordAlgo(user.passwordAlgo());
        existing.setUpdatedAt(OffsetDateTime.now());

        var saved = jpaRepository.saveAndFlush(existing);
        return toDomain(saved);
    }

    @Override
    public void delete(UUID id) {
        try {
            jpaRepository.deleteById(id);
        } catch (EmptyResultDataAccessException ignored) {
            // idempotent delete: ignore if not found
        }
    }
}
