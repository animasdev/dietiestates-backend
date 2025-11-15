package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Query("SELECT r FROM RefreshTokenEntity r WHERE r.tokenHash = :hash AND r.revokedAt IS NULL AND r.expiresAt > :now")
    Optional<RefreshTokenEntity> findActiveByHash(@Param("hash") String hash, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :when, r.updatedAt = :when WHERE r.userId = :userId AND r.revokedAt IS NULL")
    void revokeAllForUser(@Param("userId") UUID userId, @Param("when") OffsetDateTime when);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :when, r.updatedAt = :when WHERE r.tokenHash = :hash AND r.revokedAt IS NULL")
    void revokeByHash(@Param("hash") String hash, @Param("when") OffsetDateTime when);
}
