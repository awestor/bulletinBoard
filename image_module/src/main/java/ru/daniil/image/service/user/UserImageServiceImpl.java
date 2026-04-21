package ru.daniil.image.service.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.image.repository.UserImageRepository;
import ru.daniil.image.service.PrepareImageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Transactional
public class UserImageServiceImpl extends PrepareImageService implements UserImageService {
    private final UserImageRepository userImageRepository;
    private static final Logger methodLogger = LoggerFactory.getLogger("METHOD-LOGGER");

    @Value("${file.user.upload-dir}")
    private String uploadDir;

    public UserImageServiceImpl(UserImageRepository userImageRepository) {
        this.userImageRepository = userImageRepository;
    }

    @Cacheable(value = "userImages",
            key = "#fileName", unless = "#result == null")
    @Override
    public String saveImage(MultipartFile file, String email) {
        String fileName = getFileUploadedName(file);
        try{
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            methodLogger.info("Файл сохранен: {}, размер: {} байт, путь: {}",
                    fileName, file.getSize(), filePath);

            userImageRepository.saveImage(fileName, email);
        return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Возникла проблема при сохранении изображения: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "userImages",
            key = "#fileName", unless = "#result == null")
    public String completePath(String filename) {
        if (filename == null || filename.isEmpty()){
            methodLogger.info("Переданное название файла = null");
            return null;
        }
        methodLogger.info("Переданное название файла = {}", filename);
        Path path = Paths.get(uploadDir, filename);
        if (!Files.exists(path)) {
            throw new BadCredentialsException("Изображение пользовательского аватара не найдено: " + filename);
        }
        return path.toString();
    }

    @Override
    @CacheEvict(value = "userImages", key = "#fileName")
    public void deleteUserAvatar(String email, String imageName) {
        try {
            deleteImageFileOrigin(imageName, uploadDir);
            userImageRepository.clearImage(email);
        } catch (RuntimeException e){
            methodLogger.warn("Попытка удалить файл аватарки пользователя не увенчалась успехом");
            throw e;
        }
    }
}
