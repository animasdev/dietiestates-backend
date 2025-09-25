package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.auth;

import it.dieti.dietiestatesbackend.domain.auth.SignUpToken;
import it.dieti.dietiestatesbackend.domain.auth.SignUpTokenRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class SignUpTokenRepositoryJpaAdapter implements SignUpTokenRepository {
    private final SignUpTokenJpaRepository jpaRepository;

    public SignUpTokenRepositoryJpaAdapter(SignUpTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<SignUpToken> findActiveByEmail(String email, OffsetDateTime now) {
        return jpaRepository.findActiveByEmail(email, now).map(this::toDomain);
    }

    @Override
    public Optional<SignUpToken> findActiveByToken(String token, OffsetDateTime now) {
        return jpaRepository.findActiveByToken(token, now).map(this::toDomain);
    }

    @Override
    public SignUpToken insert(SignUpToken token) {
        var entity = toEntity(token);
        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public SignUpToken update(SignUpToken token) {
        //for now update and insert have same implementation
        return insert(token);
    }

    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }

    private SignUpToken toDomain(SignUpTokenEntity e) {
        return new SignUpToken(
                e.getId(),
                e.getEmail(),
                e.getDisplayName(),
                e.getToken(),
                e.getExpiresAt(),
                e.getConsumedAt(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    private SignUpTokenEntity toEntity(SignUpToken d) {
        var e = new SignUpTokenEntity();
        e.setId(d.id());
        e.setEmail(d.email());
        e.setDisplayName(d.displayName());
        e.setToken(d.token());
        e.setExpiresAt(d.expiresAt());
        e.setConsumedAt(d.consumedAt());
        e.setCreatedAt(d.createdAt());
        e.setUpdatedAt(d.updatedAt());
        return e;
    }
}

