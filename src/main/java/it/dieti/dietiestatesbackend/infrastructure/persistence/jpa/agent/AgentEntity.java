package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.agent;

import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.agency.AgencyEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media.MediaAssetEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "agents")
@Getter
@Setter
public class AgentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private AgencyEntity agency;

    @Column(name = "rea_number", nullable = false)
    private String reaNumber;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "profile_photo_media_id")
    private MediaAssetEntity profilePhotoMedia;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
