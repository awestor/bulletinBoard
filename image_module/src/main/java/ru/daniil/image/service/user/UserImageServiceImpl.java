package ru.daniil.image.service.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
public class UserImageServiceImpl implements UserImageService {
    private final UserImageRepository userImageRepository;
    private final PrepareImageService prepareImageService;
    private static final Logger methodLogger = LoggerFactory.getLogger("METHOD-LOGGER");

    @Value("${file.user.upload-dir}")
    private String uploadDir;

    public UserImageServiceImpl(UserImageRepository userImageRepository,
                                PrepareImageService prepareImageService) {
        this.userImageRepository = userImageRepository;
        this.prepareImageService = prepareImageService;
    }

    @Cacheable(value = "userImages",
            key = "#username + '_' + #file.getOriginalFilename()", unless = "#result == null")
    public String saveImage(MultipartFile file, String username) throws IOException {
        String fileName = prepareImageService.getFileUploadedName(file);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        methodLogger.info("Файл сохранен: {}, размер: {} байт, путь: {}",
                fileName, file.getSize(), filePath);

        userImageRepository.saveImage(fileName, username);
        return filePath.toString();
    }

    public String completePath(String filename) {
        Path path = Paths.get(uploadDir, filename);
        if (!Files.exists(path)) {
            throw new BadCredentialsException("Изображение пользовательского аватара не найдено: " + filename);
        }
        return path.toString();
    }
}
