package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing;

import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.agent.AgentEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.agency.AgencyEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "listings")
@Getter
@Setter
public class ListingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private AgencyEntity agency;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "owner_agent_id", nullable = false)
    private AgentEntity ownerAgent;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "listing_type_id", nullable = false)
    private ListingTypeEntity listingType;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private ListingStatusEntity status;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @Column(nullable = false)
    private String currency;

    @Column(name = "size_sqm")
    private java.math.BigDecimal sizeSqm;

    @Column
    private Integer rooms;

    @Column
    private Integer floor;

    @Column(name = "energy_class")
    private String energyClass;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    @Column(nullable = false)
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(columnDefinition = "geography(Point,4326)", nullable = false)
    private Point geo;

    @Column(name = "pending_delete_until")
    private OffsetDateTime pendingDeleteUntil;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
