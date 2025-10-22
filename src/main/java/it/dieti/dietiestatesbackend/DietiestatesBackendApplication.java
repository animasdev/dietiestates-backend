package it.dieti.dietiestatesbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DietiestatesBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DietiestatesBackendApplication.class, args);
    }

}
