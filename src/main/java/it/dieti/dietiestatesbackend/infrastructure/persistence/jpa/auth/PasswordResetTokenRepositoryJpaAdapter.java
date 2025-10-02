package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.auth;

import it.dieti.dietiestatesbackend.domain.auth.PasswordResetToken;
import it.dieti.dietiestatesbackend.domain.auth.PasswordResetTokenRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class PasswordResetTokenRepositoryJpaAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository repo;

    public PasswordResetTokenRepositoryJpaAdapter(PasswordResetTokenJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<PasswordResetToken> findActiveByUser(UUID userId, OffsetDateTime now) {
        return repo.findActiveByUser(userId, now).map(this::toDomain);
    }

    @Override
    public Optional<PasswordResetToken> findActiveByToken(String token, OffsetDateTime now) {
        return repo.findActiveByToken(token, now).map(this::toDomain);
    }

    @Override
    public PasswordResetToken insert(PasswordResetToken token) {
        var e = toEntity(token);
        var saved = repo.save(e);
        return toDomain(saved);
    }

    @Override
    public PasswordResetToken update(PasswordResetToken token) {
        return insert(token);
    }

    @Override
    public void delete(UUID id) {
        repo.deleteById(id);
    }

    private PasswordResetToken toDomain(PasswordResetTokenEntity e) {
        return new PasswordResetToken(
                e.getId(), e.getUserId(), e.getToken(), e.getExpiresAt(), e.getConsumedAt(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private PasswordResetTokenEntity toEntity(PasswordResetToken d) {
        var e = new PasswordResetTokenEntity();
        e.setId(d.id());
        e.setUserId(d.userId());
        e.setToken(d.token());
        e.setExpiresAt(d.expiresAt());
        e.setConsumedAt(d.consumedAt());
        e.setCreatedAt(d.createdAt());
        e.setUpdatedAt(d.updatedAt());
        return e;
    }
}

