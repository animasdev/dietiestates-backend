package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user;

import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.role.RoleEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity @Table(name = "users")
@Getter
@Setter
public class UserEntity {
    @Id
    @Column(nullable = false)
    private UUID  id;
    @Column(nullable = false, name = "display_name")
    private String displayName;
    // PostgreSQL citext (case-insensitive). Keep aligned with V3__users.sql
    @Column(nullable = false, columnDefinition = "citext")
    private String email;
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name="role_id", nullable=false)
    private RoleEntity role;
    @Column(name = "password_hash")
    private String passwordHash;
    @Column(name = "password_algo")
    private String passwordAlgo;
    @Column(nullable = false, name = "is_first_access")
    Boolean firstAccess;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

}
