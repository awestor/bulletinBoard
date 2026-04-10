package ru.daniil.image.service.product;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.product.ProductImage;
import ru.daniil.image.repository.ProductImageRepository;
import ru.daniil.image.service.PrepareImageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Transactional
public class ProductImageServiceImpl implements ProductImageService {
    private final ProductImageRepository productImageRepository;
    private final PrepareImageService prepareImageService;

    @Value("${file.product.upload-dir}")
    private String uploadDir;

    public ProductImageServiceImpl(ProductImageRepository productImageRepository, PrepareImageService prepareImageService) {
        this.productImageRepository = productImageRepository;
        this.prepareImageService = prepareImageService;
    }

    public String saveImage(MultipartFile file) throws IOException {
        String fileName = prepareImageService.getFileUploadedName(file);

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

    public void save(ProductImage productImage) {
        productImageRepository.save(productImage);
    }
}
