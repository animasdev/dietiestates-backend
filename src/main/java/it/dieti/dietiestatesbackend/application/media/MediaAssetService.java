package it.dieti.dietiestatesbackend.application.media;

import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.domain.media.MediaAsset;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetCategoryRepository;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetRepository;
import jakarta.validation.Valid;
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
    private static final String LISTING_PHOTO = "LISTING_PHOTO";
    public static final String CATEGORY_CODE = "categoryCode";

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
            log.warn("Tentativo di upload media senza utente autenticato");
            throw UnauthorizedException.bearerTokenMissing();
        }

        var normalizedCategory = normalize(categoryCode);
        var category = categoryRepository.findByCode(normalizedCategory)
                .orElseThrow(() -> {
                    log.warn("Categoria media non valida '{}' per user {}", normalizedCategory, userId);
                    return BadRequestException.forField(CATEGORY_CODE, "Categoria media non valida.");
                });

        validateFile(file);
        MediaStorageClient.StoredMedia stored;
        stored = storageClient.store(normalizedCategory, file);
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
            log.warn("Upload media senza file allegato");
            throw BadRequestException.forField("file", "Il campo 'file' è obbligatorio.");
        }
        if (file.getSize() > MAX_BYTES) {
            log.warn("Upload media con dimensione {} bytes oltre il limite", file.getSize());
            throw BadRequestException.forField("file", "Il file supera la dimensione massima consentita (5MB).");
        }
        var contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Upload media con content-type non supportato: {}", contentType);
            throw BadRequestException.forField("file", "Formato file non supportato. Usa JPEG, PNG o WEBP.");
        }
    }

    private String normalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            log.warn("Upload media con categoryCode mancante");
            throw BadRequestException.forField(CATEGORY_CODE, "Il campo 'categoryCode' è obbligatorio.");
        }
        return input.trim().toUpperCase();
    }

    public MediaAsset getListingPhoto(@Valid UUID id) {
        var normalizedCategory = normalize(LISTING_PHOTO);
        var category = categoryRepository.findByCode(normalizedCategory)
                .orElseThrow(() -> {
                    log.warn("Categoria media non valida '{}'", normalizedCategory);
                    return BadRequestException.forField(CATEGORY_CODE, "Categoria media non valida.");
                });
        return mediaAssetRepository.getByIdAndCategory(id,category.id());
    }
}
