package ru.daniil.image.service.user;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UserImageService {

    String saveImage(MultipartFile file, String username);

    String completePath(String filename);
}
