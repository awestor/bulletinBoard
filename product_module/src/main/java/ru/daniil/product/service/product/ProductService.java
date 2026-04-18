package ru.daniil.product.service.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.response.product.ProductFilterRequest;

import java.util.List;

public interface ProductService {

    Product save(Product product);

    //Product create(CreateProductRequest request, Category category);

    //Product update(Long id, CreateProductRequest request, Category newCategory);

    Product getById(Long id);

    Product getBySku(String sku);

    List<Product> getByCategory(String categoryName);

    String generateSku();

    Page<Product> getBySeller(Long userId, Pageable pageable);

    Page<Product> filterProducts(ProductFilterRequest filter, Pageable pageable);

    long count();

    void updateStock(Long id, Integer quantity);

    void updateCategoryForProducts(List<Product> products, Category newCategory);

    void delete(Product product);

    /**
     * Осторожно!!! Данный метод удаляет прям все файлы размещённых в системе продуктов
     */
    void deleteAll();
}
