package it.dieti.dietiestatesbackend.infrastructure.storage;

import it.dieti.dietiestatesbackend.application.media.MediaStorageClient;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Component
public class LocalFilesystemMediaStorage implements MediaStorageClient {
    private final MediaStorageProperties properties;

    public LocalFilesystemMediaStorage(MediaStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredMedia store(String categoryCode, MultipartFile file) {
        try {
            Destination destination = resolveDestination(categoryCode, file);
            writeFile(file, destination);
            ImageDimensions dimensions = readDimensions(destination);
            String publicUrl = buildPublicUrl(destination.relativePath());
            String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            return new StoredMedia(destination.relativePath().toString().replace('\\', '/'), publicUrl, dimensions.width(), dimensions.height(), mimeType);
        } catch (IOException ex) {
            throw new LocalStorageException("Failed to store media file", ex);
        }
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        Path basePath = resolveBasePath();
        Path target = basePath.resolve(storagePath).normalize();
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new LocalStorageException("Failed to delete media file: " + storagePath, ex);
        }
    }

    private Destination resolveDestination(String categoryCode, MultipartFile file) {
        Path basePath = resolveBasePath();
        String sanitizedCategory = sanitize(categoryCode);
        String extension = resolveExtension(file);
        String filename = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
        Path relative = Paths.get(sanitizedCategory, filename);
        Path absolute = basePath.resolve(relative).normalize();
        return new Destination(relative, absolute);
    }

    private void writeFile(MultipartFile file, Destination destination) throws IOException {
        Files.createDirectories(destination.absolutePath().getParent());
        file.transferTo(destination.absolutePath());
    }

    private ImageDimensions readDimensions(Destination destination) {
        try {
            BufferedImage image = ImageIO.read(destination.absolutePath().toFile());
            if (image != null) {
                return new ImageDimensions(image.getWidth(), image.getHeight());
            }
        } catch (IOException ignored) {
            // best effort, dimensions optional
        }
        return new ImageDimensions(null, null);
    }

    private Path resolveBasePath() {
        return Paths.get(properties.getLocal().getBasePath()).toAbsolutePath();
    }

    private String buildPublicUrl(Path relativePath) {
        String publicBase = properties.getPublicBaseUrl();
        if (!publicBase.startsWith("/")) {
            publicBase = "/" + publicBase;
        }
        if (publicBase.endsWith("/")) {
            publicBase = publicBase.substring(0, publicBase.length() - 1);
        }
        String normalized = relativePath.toString().replace('\\', '/');
        return publicBase + "/" + normalized;
    }

    private String sanitize(String category) {
        if (category == null || category.isBlank()) {
            return "misc";
        }
        return category.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
    }

    private String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            String ext = original.substring(original.lastIndexOf('.') + 1);
            if (!ext.isBlank()) {
                return ext.toLowerCase(Locale.ROOT);
            }
        }
        String contentType = file.getContentType();
        if (contentType == null) return "";
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "";
        };
    }

    private record Destination(Path relativePath, Path absolutePath) {}
    private record ImageDimensions(Integer width, Integer height) {}
}
