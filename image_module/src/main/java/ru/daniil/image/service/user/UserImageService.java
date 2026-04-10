package ru.daniil.image.service.user;

import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.product.ProductImage;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface UserImageService {

    String saveImage(MultipartFile file, String username) throws IOException;

    String completePath(String filename);
}
