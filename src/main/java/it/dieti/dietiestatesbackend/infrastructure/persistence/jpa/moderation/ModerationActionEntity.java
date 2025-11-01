package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.moderation;

import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing.ListingEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "listing_moderation_actions")
@Getter
@Setter
public class ModerationActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private ListingEntity listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private UserEntity performedBy;

    @Column(name = "performed_by_role", nullable = false)
    private String performedByRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_type_id", nullable = false)
    private ModerationActionTypeEntity actionType;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
