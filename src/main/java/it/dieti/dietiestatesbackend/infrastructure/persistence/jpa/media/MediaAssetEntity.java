package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media;

import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "media_assets")
@Getter
@Setter
public class MediaAssetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MediaAssetCategoryEntity category;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "public_url", nullable = false)
    private String publicUrl;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "width_px")
    private Integer widthPx;

    @Column(name = "height_px")
    private Integer heightPx;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;
}
