package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.feature;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "features")
@Getter @Setter
public class FeatureEntity {
    @Id @Column(nullable = false)
    private UUID id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
