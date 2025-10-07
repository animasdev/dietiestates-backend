package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.agency;

import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media.MediaAssetEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "agencies")
@Getter
@Setter
public class AgencyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "logo_media_id")
    private MediaAssetEntity logoMedia;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "approved_by")
    private UserEntity approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
