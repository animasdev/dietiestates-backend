package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "roles")
@Getter
@Setter
public class RoleEntity {
    @Id
    @Column(nullable = false)
    private UUID id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String code;
    @Column
    private String description;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
