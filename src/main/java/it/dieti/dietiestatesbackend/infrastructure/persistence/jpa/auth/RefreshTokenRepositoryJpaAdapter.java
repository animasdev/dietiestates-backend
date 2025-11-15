package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.auth;

import it.dieti.dietiestatesbackend.domain.auth.RefreshToken;
import it.dieti.dietiestatesbackend.domain.auth.RefreshTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
public class RefreshTokenRepositoryJpaAdapter implements RefreshTokenRepository {
    private final RefreshTokenJpaRepository repo;

    public RefreshTokenRepositoryJpaAdapter(RefreshTokenJpaRepository repo) {
        this.repo = repo;
    }

    private static RefreshTokenEntity toEntity(RefreshToken d) {
        RefreshTokenEntity e = new RefreshTokenEntity();
        e.setId(d.id());
        e.setUserId(d.userId());
        e.setTokenHash(d.tokenHash());
        e.setExpiresAt(d.expiresAt());
        e.setRevokedAt(d.revokedAt());
        e.setCreatedAt(d.createdAt());
        e.setUpdatedAt(d.updatedAt());
        return e;
    }

    private static RefreshToken toDomain(RefreshTokenEntity e) {
        return new RefreshToken(
                e.getId(), e.getUserId(), e.getTokenHash(), e.getExpiresAt(), e.getRevokedAt(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public RefreshToken insert(RefreshToken token) {
        var saved = repo.save(toEntity(token));
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void update(RefreshToken token) {
        repo.save(toEntity(token));
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
        return repo.findByTokenHash(tokenHash).map(RefreshTokenRepositoryJpaAdapter::toDomain);
    }

    @Override
    public Optional<RefreshToken> findActiveByHash(String tokenHash, OffsetDateTime now) {
        return repo.findActiveByHash(tokenHash, now).map(RefreshTokenRepositoryJpaAdapter::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllForUser(java.util.UUID userId, OffsetDateTime when) {
        repo.revokeAllForUser(userId, when);
    }

    @Override
    @Transactional
    public void revokeByHash(String tokenHash, OffsetDateTime when) {
        repo.revokeByHash(tokenHash, when);
    }
}
