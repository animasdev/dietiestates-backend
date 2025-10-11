package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media.listing;

import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing.ListingEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media.MediaAssetEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table( name = "listing_media")
@Getter
@Setter
public class ListingMediaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private ListingEntity listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private MediaAssetEntity media;

    @Column(name= "sort_order",nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
