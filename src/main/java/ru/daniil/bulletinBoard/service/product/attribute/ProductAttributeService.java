package ru.daniil.bulletinBoard.service.product.attribute;

import ru.daniil.bulletinBoard.entity.base.product.Product;
import ru.daniil.bulletinBoard.entity.base.product.ProductAttribute;

import java.util.Map;
import java.util.Set;

public interface ProductAttributeService {
    ProductAttribute save(Product product, String key, String value);

    Set<ProductAttribute> saveMany(Product product, Map<String, String> data);

    Set<ProductAttribute> setMany(Product product, Map<String, String> newAttributes);

    void deleteAttribute(ProductAttribute attribute);

    void deleteAttributeByName(String key);
}
