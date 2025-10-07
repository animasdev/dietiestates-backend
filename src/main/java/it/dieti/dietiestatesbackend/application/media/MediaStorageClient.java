package it.dieti.dietiestatesbackend.application.media;

import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageClient {
    StoredMedia store(String categoryCode, MultipartFile file);
    void delete(String storagePath);

    record StoredMedia(String storagePath, String publicUrl, Integer widthPx, Integer heightPx, String mimeType) {}
}
