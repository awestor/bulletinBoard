package ru.daniil.image.service.product;

import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.product.ProductImage;

import java.io.IOException;

public interface ProductImageService {

    String saveImage(MultipartFile file) throws IOException;

    void save(ProductImage productImage);
}
