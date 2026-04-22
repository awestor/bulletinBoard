package ru.daniil.app.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Profile("!test")
public class DotenvConfig {

    @Value("${dotenv.path:}")
    private String envPath;

    @Value("${dotenv.filename:.env}")
    private String filename;

    @PostConstruct
    public void init() {
        String finalPath = resolvePath();

        if (finalPath == null || finalPath.isBlank()) {
            return;
        }

        Path path = Paths.get(finalPath);
        if (!Files.exists(path)) {
            return;
        }

        Dotenv dotenv = Dotenv.configure()
                .directory(path.getParent().toString())
                .filename(path.getFileName().toString())
                .load();

        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );
    }

    private String resolvePath() {
        String path = System.getenv("DOTENV_PATH");
        if (path == null || path.isBlank()) {
            path = System.getProperty("dotenv.path");
        }
        if (path == null || path.isBlank()) {
            path = envPath;
        }

        if (path == null || path.isBlank()) {
            return findDefaultEnvPath();
        }

        // Обработка специального значения ".."
        if ("..".equals(path)) {
            Path parentDir = Paths.get("").toAbsolutePath().getParent();
            if (parentDir != null) {
                return parentDir.resolve(filename).toString();
            }
            return null;
        }

        return path;
    }

    // Поиск по стандарту, что охватывает родительскую и текущую директории
    private String findDefaultEnvPath() {
        Path current = Paths.get("").toAbsolutePath();

        Path currentEnv = current.resolve(filename);
        if (Files.exists(currentEnv)) {
            return currentEnv.toString();
        }

        Path parentEnv = current.getParent().resolve(filename);
        if (Files.exists(parentEnv)) {
            return parentEnv.toString();
        }

        return null;
    }
}