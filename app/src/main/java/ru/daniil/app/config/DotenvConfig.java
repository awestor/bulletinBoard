package ru.daniil.app.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class DotenvConfig {

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @PostConstruct
    public void init() {
        if ("test".equals(activeProfile) || "docker".equals(activeProfile)) {
            return;
        }

        Dotenv dotenv = Dotenv.load();
        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );
    }
}

