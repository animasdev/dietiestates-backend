package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    @Query("select t from PasswordResetTokenEntity t where t.userId = :userId and t.consumedAt is null and t.expiresAt > :now")
    Optional<PasswordResetTokenEntity> findActiveByUser(UUID userId, OffsetDateTime now);

    @Query("select t from PasswordResetTokenEntity t where t.token = :token and t.consumedAt is null and t.expiresAt > :now")
    Optional<PasswordResetTokenEntity> findActiveByToken(String token, OffsetDateTime now);
}

