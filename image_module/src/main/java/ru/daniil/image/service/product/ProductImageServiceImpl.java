package ru.daniil.image.service.product;

import jakarta.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductImageServiceImpl implements ProductImageService {
    private final ProductImageRepository productImageRepository;
    private final PrepareImageService prepareImageService;
    private static final Logger methodLogger = LoggerFactory.getLogger("METHOD-LOGGER");

    @Value("${file.product.upload-dir}")
    private String uploadDir;

    public ProductImageServiceImpl(ProductImageRepository productImageRepository,
                                   PrepareImageService prepareImageService) {
        this.productImageRepository = productImageRepository;
        this.prepareImageService = prepareImageService;
    }

    @Override
    public List<String> getProductImages(String sku) {
        return productImageRepository.findByProductSku(sku)
                .stream()
                .map(image -> completePath(image.getName()))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "productImages",
            key = "T(org.apache.commons.codec.digest.DigestUtils).md5Hex(#file.getBytes())",
            unless = "#result == null")
    public String saveImage(MultipartFile file) throws RuntimeException{
        String fileName = prepareImageService.getFileUploadedName(file);
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

    @Override
    public void deleteImage(String filename){
        ProductImage image =  productImageRepository.findByName(filename).orElseThrow(
                () -> new NotFoundException("Данного изображения не найдено в БД")
        );
        try {
            productImageRepository.delete(image);
            deleteImageFileOrigin(filename);
        } catch (Exception e){
            methodLogger.error("Удаление с файлом не удалось");
        }
    }

    @Override
    public void deleteAllImages(Long productId){
        List<ProductImage> images = productImageRepository.findByProductId(productId);
        for (ProductImage image : images){
            deleteImageFileOrigin(image.getName());
        }
        productImageRepository.deleteByProductId(productId);
    }

    /**
     * Удаляет файл из локального хранилища на устройстве
     * @param filename название файла
     */
    private void deleteImageFileOrigin(String filename) {
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

    @Override
    public void setMain(String fileName) {
        productImageRepository.unsetOtherMainImages(fileName);
        productImageRepository.setMainByFileName(fileName);
    }
}
