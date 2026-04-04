package ru.daniil.user.service.product_module.product.image;

import jakarta.validation.ValidationException;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.product.ProductImage;
import ru.daniil.user.repository.product.ProductImageRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProductImageServiceImpl implements ProductImageService {
    private final ProductImageRepository productImageRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    public ProductImageServiceImpl(ProductImageRepository productImageRepository) {
        this.productImageRepository = productImageRepository;
    }

    public String saveImage(MultipartFile file) throws IOException {
        String fileName = getFileUploadedName(file);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        System.out.printf("Файл сохранен: %s, размер: %d байт, путь: %s%n",
                fileName, file.getSize(), filePath);

        return fileName;
    }

    private static @NonNull String getFileUploadedName(MultipartFile file) {
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

    public void save(ProductImage productImage) {
        productImageRepository.save(productImage);
    }
}
