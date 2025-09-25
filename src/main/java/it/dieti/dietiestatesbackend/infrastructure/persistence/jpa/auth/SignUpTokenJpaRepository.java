package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SignUpTokenJpaRepository extends JpaRepository<SignUpTokenEntity, UUID> {
    @Query("select t from SignUpTokenEntity t where lower(t.email) = lower(?1) and t.consumedAt is null and t.expiresAt > ?2")
    Optional<SignUpTokenEntity> findActiveByEmail(String email, OffsetDateTime now);

    @Query("select t from SignUpTokenEntity t where t.token = ?1 and t.consumedAt is null and t.expiresAt > ?2")
    Optional<SignUpTokenEntity> findActiveByToken(String token, OffsetDateTime now);
}

