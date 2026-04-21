package ru.daniil.image.service.user;

import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.user.User;

import java.io.IOException;

public interface UserImageService {

    String saveImage(MultipartFile file, String email);

    String completePath(String filename);

    void deleteUserAvatar(String email, String imageName);
}
