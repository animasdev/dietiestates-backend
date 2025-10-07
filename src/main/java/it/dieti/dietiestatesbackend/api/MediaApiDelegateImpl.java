package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.MediaUploadResponse;
import it.dieti.dietiestatesbackend.application.media.MediaAssetService;
import it.dieti.dietiestatesbackend.domain.media.MediaAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

@Service
public class MediaApiDelegateImpl implements MediaApiDelegate {
    private static final Logger log = LoggerFactory.getLogger(MediaApiDelegateImpl.class);
    private final MediaAssetService mediaAssetService;

    public MediaApiDelegateImpl(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    @Override
    public ResponseEntity<MediaUploadResponse> mediaUploadsPost(String categoryCode, MultipartFile file) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var userId = UUID.fromString(jwtAuth.getToken().getSubject());
        try {
            MediaAsset asset = mediaAssetService.upload(userId, categoryCode, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(asset));
        } catch (IllegalArgumentException ex) {
            log.warn("mediaUploadsPost validation failed for user {}: {}", userId, ex.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            log.error("mediaUploadsPost failed for user {}", userId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private MediaUploadResponse toResponse(MediaAsset asset) {
        MediaUploadResponse response = new MediaUploadResponse();
        response.setAssetId(asset.id());
        response.setPublicUrl(URI.create(asset.publicUrl()));
        response.setMimeType(asset.mimeType());
        response.setWidthPx(asset.widthPx());
        response.setHeightPx(asset.heightPx());
        return response;
    }
}
