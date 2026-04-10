package ru.daniil.image.service;

import jakarta.validation.ValidationException;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class PrepareImageService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    public @NonNull String getFileUploadedName(MultipartFile file) {
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
}
