package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.media;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "media_asset_categories")
@Getter
@Setter
public class MediaAssetCategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String description;
}
