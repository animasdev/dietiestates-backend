package it.dieti.dietiestatesbackend.application.media;

import it.dieti.dietiestatesbackend.domain.media.MediaAsset;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetCategoryRepository;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaAssetService {
    private static final Logger log = LoggerFactory.getLogger(MediaAssetService.class);
    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5MB
    private static final Set<String> SUPPORTED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final MediaAssetRepository mediaAssetRepository;
    private final MediaAssetCategoryRepository categoryRepository;
    private final MediaStorageClient storageClient;

    public MediaAssetService(MediaAssetRepository mediaAssetRepository,
                             MediaAssetCategoryRepository categoryRepository,
                             MediaStorageClient storageClient) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.categoryRepository = categoryRepository;
        this.storageClient = storageClient;
    }

    @Transactional
    public MediaAsset upload(UUID userId, String categoryCode, MultipartFile file) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        var normalizedCategory = normalize(categoryCode);
        var category = categoryRepository.findByCode(normalizedCategory)
                .orElseThrow(() -> new IllegalArgumentException("Unknown media category: " + categoryCode));

        validateFile(file);

        MediaStorageClient.StoredMedia stored;
        try {
            stored = storageClient.store(normalizedCategory, file);
        } catch (RuntimeException ex) {
            log.error("Failed to store media for user {} and category {}", userId, normalizedCategory, ex);
            throw ex;
        }

        var asset = new MediaAsset(
                null,
                category.id(),
                stored.storagePath(),
                stored.publicUrl(),
                stored.mimeType(),
                stored.widthPx(),
                stored.heightPx(),
                userId,
                OffsetDateTime.now()
        );

        return mediaAssetRepository.save(asset);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("File exceeds maximum allowed size (5MB)");
        }
        var contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported media type: " + contentType);
        }
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toUpperCase();
    }
}
