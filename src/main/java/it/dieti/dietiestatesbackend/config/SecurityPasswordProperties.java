package it.dieti.dietiestatesbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityPasswordProperties {
    private String passwordAlgo = "bcrypt";
    private final Bcrypt bcrypt = new Bcrypt();

    public String getPasswordAlgo() {
        return passwordAlgo;
    }

    public void setPasswordAlgo(String passwordAlgo) {
        this.passwordAlgo = passwordAlgo;
    }

    public Bcrypt getBcrypt() {
        return bcrypt;
    }

    public static class Bcrypt {
        private Integer strength = 10;

        public Integer getStrength() {
            return strength;
        }

        public void setStrength(Integer strength) {
            this.strength = strength;
        }
    }
}

