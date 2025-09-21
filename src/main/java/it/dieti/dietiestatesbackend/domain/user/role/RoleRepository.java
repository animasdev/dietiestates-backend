package it.dieti.dietiestatesbackend.domain.user.role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    List<Role> findAll();
    Optional<Role> findByCode(String code);
}
