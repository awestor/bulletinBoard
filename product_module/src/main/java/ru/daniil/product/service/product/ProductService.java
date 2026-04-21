package ru.daniil.product.service.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.response.product.ProductFilterRequest;

import java.util.List;
import java.util.Map;

public interface ProductService {

    Product save(Product product);

    Product getById(Long id);

    Product getBySku(String sku);

    List<Product> getByCategory(String categoryName);

    String generateSku();

    Page<Product> getBySeller(Long userId, Pageable pageable);

    Page<Product> filterProducts(ProductFilterRequest filter, Pageable pageable);

    long count();

    /**
     * Обновление остатков нескольких товаров
     * @param skuQuantityMap мапа SKU + количество для списания для каждого продукта
     * @throws IllegalArgumentException если для какого-то товара недостаточно остатка
     */
    void decreaseStockQuantities(Map<String, Integer> skuQuantityMap);

    /**
     * Восстановление остатков нескольких товаров (при отмене оплаты)
     * @param skuQuantityMap мапа SKU + количество для восстановления для каждого продукта
     */
    void increaseStockQuantities(Map<String, Integer> skuQuantityMap);

    void updateStock(Long id, Integer quantity);

    void updateCategoryForProducts(List<Product> products, Category newCategory);

    void delete(Product product);

    /**
     * Осторожно!!! Данный метод удаляет прям все файлы размещённых в системе продуктов
     */
    void deleteAll();
}
