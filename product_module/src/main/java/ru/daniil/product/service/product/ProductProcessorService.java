package ru.daniil.product.service.product;

import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.CreateUpdateProductRequest;

public interface ProductProcessorService {
    Product create(CreateUpdateProductRequest request, User user);

    Product update(Long id, CreateUpdateProductRequest request);

    void delete(Long id);

    void deleteAll();
}
