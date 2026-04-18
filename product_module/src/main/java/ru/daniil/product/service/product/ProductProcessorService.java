package ru.daniil.product.service.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.CreateProductRequest;
import ru.daniil.core.response.product.ProductFilterRequest;

public interface ProductProcessorService {
    Product create(CreateProductRequest request, User user);

    Product update(Long id, CreateProductRequest request, Category newCategory);

    void delete(Long id);

    void deleteAll();
}
