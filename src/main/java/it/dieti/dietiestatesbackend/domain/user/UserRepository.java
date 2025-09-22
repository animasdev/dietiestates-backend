package it.dieti.dietiestatesbackend.domain.user;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    List<User> findAll(Pageable pageable);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    User insert(User user);
    boolean existsByEmail(String email);
    User update(User user);
    void delete(UUID id);
}
