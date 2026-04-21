package ru.daniil.image.service;

import jakarta.validation.ValidationException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class PrepareImageService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final Logger methodLogger = LoggerFactory.getLogger("METHOD-LOGGER");

    protected @NonNull String getFileUploadedName(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new ValidationException("Необходимо загрузить хотя бы одно изображение");

        if (file.getSize() > MAX_FILE_SIZE)
            throw new ValidationException(String.format("Файл превышает максимальный размер %dMB",
                    MAX_FILE_SIZE / (1024 * 1024)));

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType()))
            throw new ValidationException(String.format("Файл имеет неподдерживаемый тип: %s. " +
                    "Допустимые типы: JPEG, PNG, GIF, WEBP", file.getContentType()));

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        return UUID.randomUUID().toString() + fileExtension;
    }

    protected void deleteImageFileOrigin(String filename, String uploadDir) {
        if (filename == null || filename.isBlank()) {
            methodLogger.warn("Переданное название файла = null или пустое");
            return;
        }

        try {
            Path path = Paths.get(uploadDir, filename);

            if (!Files.exists(path)) {
                methodLogger.warn("Файл для удаления не найден: {}", filename);
                return;
            }

            Files.delete(path);

        } catch (IOException e) {
            methodLogger.error("Ошибка при удалении файла {}: {}", filename, e.getMessage());
            throw new RuntimeException("Не удалось удалить файл изображения: " + filename, e);
        }
    }
}
