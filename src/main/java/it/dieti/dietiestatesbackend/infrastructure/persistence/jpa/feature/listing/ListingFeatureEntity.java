package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.feature.listing;

import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.feature.FeatureEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing.ListingEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table( name = "listing_features")
@Getter @Setter
public class ListingFeatureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private ListingEntity listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", nullable = false)
    private FeatureEntity feature;

    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Override
    public String toString() {
        return "ListingFeatureEntity[id=" + id + ", listingId="+listing.getId()+", featureId=" + feature.getId()+"]";
    }
}
