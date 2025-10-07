package it.dieti.dietiestatesbackend.infrastructure.storage;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class MediaResourceConfiguration implements WebMvcConfigurer {
    private final MediaStorageProperties properties;

    public MediaResourceConfiguration(MediaStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!"local".equalsIgnoreCase(properties.getProvider())) {
            return;
        }
        String publicBase = properties.getPublicBaseUrl();
        if (!publicBase.startsWith("/")) {
            publicBase = "/" + publicBase;
        }
        if (!publicBase.endsWith("/")) {
            publicBase = publicBase + "/";
        }
        var location = Paths.get(properties.getLocal().getBasePath()).toAbsolutePath().toUri().toString();
        registry.addResourceHandler(publicBase + "**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
