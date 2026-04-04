package ru.daniil.user.service.product_module.product;

import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.request.CreateProductRequest;

import java.util.List;

public interface ProductService {

    Product create(CreateProductRequest request, Category category);

    Product update(Long id, CreateProductRequest request, Category newCategory);

    Product getById(Long id);

    Product getBySku(String sku);

    List<Product> getByCategory(String categoryName);

    void updateStock(Long id, Integer quantity);

    void updateCategoryForProducts(List<Product> products, Category newCategory);

    void delete(Long id);
}
