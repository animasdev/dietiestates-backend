package it.dieti.dietiestatesbackend.application.media;

import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.domain.media.MediaAsset;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetCategory;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetCategoryRepository;
import it.dieti.dietiestatesbackend.domain.media.MediaAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.DisplayName("White-box: MediaAssetService.upload (MC/DC)")
class MediaAssetServiceUploadWhiteBoxTests {

    @Mock MediaAssetRepository mediaAssetRepository;
    @Mock MediaAssetCategoryRepository categoryRepository;
    @Mock MediaStorageClient storageClient;

    private MediaAssetService service;

    @BeforeEach
    void setUp() {
        service = new MediaAssetService(mediaAssetRepository, categoryRepository, storageClient);
    }

    private MediaAssetCategory stubCategory(String codeUpper) {
        var category = new MediaAssetCategory(UUID.randomUUID(), codeUpper, codeUpper + " desc");
        when(categoryRepository.findByCode(codeUpper)).thenReturn(Optional.of(category));
        return category;
    }

    // C1: Successo nominale → E2, E4, E6, E8, E10, E12, Persistenza
    @Test
    @org.junit.jupiter.api.DisplayName("C1: success path (persist)")
    void upload_success_persistsAsset() {
        UUID userId = UUID.randomUUID();
        String categoryCode = " image ";
        var category = stubCategory("IMAGE");

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/jpeg");

        var stored = new MediaStorageClient.StoredMedia("s/path.jpg", "https://cdn/x.jpg", 100, 80, "image/jpeg");
        when(storageClient.store("IMAGE", file)).thenReturn(stored);

        var saved = new MediaAsset(UUID.randomUUID(), category.id(), stored.storagePath(), stored.publicUrl(), stored.mimeType(), stored.widthPx(), stored.heightPx(), userId, OffsetDateTime.now());
        when(mediaAssetRepository.save(any(MediaAsset.class))).thenReturn(saved);

        var result = service.upload(userId, categoryCode, file);
        assertSame(saved, result);
        verify(storageClient).store("IMAGE", file);
        verify(mediaAssetRepository).save(any(MediaAsset.class));
    }

    // C2: userId null → E1
    @Test
    @org.junit.jupiter.api.DisplayName("C2: userId null -> Unauthorized")
    void upload_userIdNull_unauthorized() {
        MultipartFile file = mock(MultipartFile.class); // won't be used
        assertThrows(UnauthorizedException.class, () -> service.upload(null, "IMAGE", file));
        verifyNoInteractions(storageClient, mediaAssetRepository);
    }

    // C3: categoryCode null/blank → E3
    @Test
    @org.junit.jupiter.api.DisplayName("C3: blank category -> BadRequest")
    void upload_blankCategory_badRequest() {
        UUID userId = UUID.randomUUID();
        assertThrows(BadRequestException.class, () -> service.upload(userId, "   ", mock(MultipartFile.class)));
        verifyNoInteractions(storageClient, mediaAssetRepository);
    }

    // C4: categoria sconosciuta → E5
    @Test
    @org.junit.jupiter.api.DisplayName("C4: unknown category -> BadRequest")
    void upload_unknownCategory_badRequest() {
        UUID userId = UUID.randomUUID();
        when(categoryRepository.findByCode("IMAGE")).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> service.upload(userId, "image", mock(MultipartFile.class)));
        verifyNoInteractions(storageClient, mediaAssetRepository);
    }

    // C5: file null → E7
    @Test
    @org.junit.jupiter.api.DisplayName("C5: file null -> BadRequest")
    void upload_fileNull_badRequest() {
        UUID userId = UUID.randomUUID();
        stubCategory("IMAGE");
        assertThrows(BadRequestException.class, () -> service.upload(userId, "image", null));
        verifyNoInteractions(storageClient, mediaAssetRepository);
    }

    // C6: file empty → E7
    @Test
    @org.junit.jupiter.api.DisplayName("C6: file empty -> BadRequest")
    void upload_fileEmpty_badRequest() {
        UUID userId = UUID.randomUUID();
        stubCategory("IMAGE");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.upload(userId, "image", file));
        verifyNoInteractions(storageClient, mediaAssetRepository);
    }

    // C7: file troppo grande (>5MB) → E9
    @Test
    @org.junit.jupiter.api.DisplayName("C7: file oversize -> BadRequest")
    void upload_fileTooBig_badRequest() {
        UUID userId = UUID.randomUUID();
        stubCategory("IMAGE");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(6L * 1024 * 1024); // > 5MB
        assertThrows(BadRequestException.class, () -> service.upload(userId, "image", file));
        verifyNoInteractions(storageClient, mediaAssetRepository);
    }

    // C8: contentType null → E11
    @Test
    @org.junit.jupiter.api.DisplayName("C8: contentType null -> BadRequest")
    void upload_contentTypeNull_badRequest() {
        UUID userId = UUID.randomUUID();
        stubCategory("IMAGE");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.upload(userId, "image", file));
        verifyNoInteractions(storageClient, mediaAssetRepository);
    }

    // C9: contentType non supportato → E11
    @Test
    @org.junit.jupiter.api.DisplayName("C9: contentType unsupported -> BadRequest")
    void upload_contentTypeUnsupported_badRequest() {
        UUID userId = UUID.randomUUID();
        stubCategory("IMAGE");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");
        assertThrows(BadRequestException.class, () -> service.upload(userId, "image", file));
        verifyNoInteractions(storageClient, mediaAssetRepository);
    }
}

