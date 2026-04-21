package ru.daniil.product.service.product;

import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.CreateUpdateProductRequest;

import java.util.List;

public interface ProductProcessorService {
    Product create(CreateUpdateProductRequest request, User user);

    List<String> addManyProductImages(String sku, List<MultipartFile> files);

    Product update(Long id, CreateUpdateProductRequest request);

    void delete(Long id);

    void deleteAll();
}
