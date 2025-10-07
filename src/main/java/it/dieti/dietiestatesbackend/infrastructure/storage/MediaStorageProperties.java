package it.dieti.dietiestatesbackend.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "media.storage")
public class MediaStorageProperties {
    private String provider = "local";
    private String publicBaseUrl = "/media";
    private final Local local = new Local();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public Local getLocal() {
        return local;
    }

    public static class Local {
        private String basePath = "storage/media";

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }
}
