package it.dieti.dietiestatesbackend.domain.auth;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SignUpTokenRepository {
    Optional<SignUpToken> findActiveByEmail(String email, OffsetDateTime now);
    Optional<SignUpToken> findActiveByToken(String token, OffsetDateTime now);
    SignUpToken insert(SignUpToken token);
    SignUpToken update(SignUpToken token);
    void delete(UUID id);
}

