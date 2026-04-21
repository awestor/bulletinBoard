package ru.daniil.image.service.product;

import jakarta.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.product.ProductImage;
import ru.daniil.image.repository.ProductImageRepository;
import ru.daniil.image.service.PrepareImageService;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductImageServiceImpl extends PrepareImageService implements ProductImageService {
    private final ProductImageRepository productImageRepository;
    private static final Logger methodLogger = LoggerFactory.getLogger("METHOD-LOGGER");

    @Value("${file.product.upload-dir}")
    private String uploadDir;

    public ProductImageServiceImpl(ProductImageRepository productImageRepository) {
        this.productImageRepository = productImageRepository;
    }

    @Override
    public Map<String, Boolean> getProductImages(String sku) {
        return productImageRepository.findByProductSku(sku)
                .stream()
                .collect(Collectors.toMap(
                        image -> completePath(image.getName()),
                        ProductImage::getIsMain
                ));
    }

    public String saveImage(MultipartFile file) throws RuntimeException{
        String fileName = getFileUploadedName(file);
        try{
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            methodLogger.debug("Файл сохранен: {}, размер: {} байт, путь: {}",
                    fileName, file.getSize(), filePath);

            return fileName;
        } catch (IOException e) {
            methodLogger.error("Возникла проблема при сохранении изображения: {}", e.getMessage());
            throw new RuntimeException("Возникла проблема при сохранении изображения: " + e.getMessage());
        }
    }

    @Cacheable(value = "productImages",
            key = "#filename",
            unless = "#result == null")
    @Override
    public String completePath(String filename) {
        if (filename == null){
            methodLogger.warn("Переданное название файла = null");
            return null;
        }
        methodLogger.info("Переданное название файла = {}", filename);
        Path path = Paths.get(uploadDir, filename);
        if (!Files.exists(path)) {
            throw new NotFoundException("Изображение продукта не найдено: " + filename);
        }
        return path.toString();
    }

    @CacheEvict(value = "productImages", key = "#filename")
    @Override
    public void deleteImage(String filename){
        ProductImage image =  productImageRepository.findByName(filename).orElseThrow(
                () -> new NotFoundException("Данного изображения не найдено в БД")
        );
        try {
            productImageRepository.delete(image);
            deleteImageFileOrigin(filename, uploadDir);
        } catch (Exception e){
            methodLogger.error("Удаление с файлом не удалось");
        }
    }

    @CacheEvict(value = "productImages", allEntries = true)
    @Override
    public void deleteAllImages(Long productId){
        List<ProductImage> images = productImageRepository.findByProductId(productId);
        for (ProductImage image : images){
            deleteImageFileOrigin(image.getName(), uploadDir);
        }
        productImageRepository.deleteByProductId(productId);
    }

    @CacheEvict(value = "productImages", allEntries = true)
    @Override
    public void deleteAllinButch() {
        try {
            productImageRepository.deleteAllInBatch();

            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                methodLogger.warn("Директория с изображениями не существует: {}", uploadDir);
                return;
            }

            int deletedCount = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadPath)) {
                for (Path filePath : stream) {
                    if (Files.isRegularFile(filePath)) {
                        try {
                            Files.delete(filePath);
                            deletedCount++;
                            methodLogger.debug("Удалён файл: {}", filePath.getFileName());
                        } catch (IOException e) {
                            methodLogger.error("Не удалось удалить файл {}: {}", filePath.getFileName(), e.getMessage());
                        }
                    }
                }
            }

            methodLogger.info("Очищена директория {}. Удалено {} файлов", uploadPath, deletedCount);

        } catch (IOException e) {
            methodLogger.error("Ошибка при очистке директории с изображениями: {}", e.getMessage());
        }
    }

    @Override
    public void save(ProductImage productImage) {
        productImageRepository.save(productImage);
    }

    @CacheEvict(value = "productImages", key = "#filename")
    @Override
    public void setMain(String fileName) {
        productImageRepository.unsetOtherMainImages(fileName);
        productImageRepository.setMainByFileName(fileName);
    }
}
