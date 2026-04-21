package ru.daniil.image.service.user;

import org.springframework.web.multipart.MultipartFile;

public interface UserImageService {

    String saveImage(MultipartFile file, String email);

    String completePath(String filename);

    void deleteUserAvatar(String email, String imageName);
}
