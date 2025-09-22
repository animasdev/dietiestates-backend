package it.dieti.dietiestatesbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {
    private String secret;
    private long expiresSeconds = 3600;
    private String issuer = "dietiestates-backend";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getExpiresSeconds() { return expiresSeconds; }
    public void setExpiresSeconds(long expiresSeconds) { this.expiresSeconds = expiresSeconds; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}

