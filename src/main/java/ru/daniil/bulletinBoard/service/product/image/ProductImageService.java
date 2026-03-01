package ru.daniil.bulletinBoard.service.product.image;

import org.springframework.web.multipart.MultipartFile;
import ru.daniil.bulletinBoard.entity.base.product.ProductImage;

import java.io.IOException;

public interface ProductImageService {

    String saveImage(MultipartFile file) throws IOException;

    void save(ProductImage productImage);
}
