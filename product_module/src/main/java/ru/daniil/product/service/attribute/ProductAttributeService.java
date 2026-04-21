package ru.daniil.product.service.attribute;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.product.ProductAttribute;

import java.util.Map;
import java.util.Set;

public interface ProductAttributeService {
    Page<ProductAttribute> getMany(Product product, Pageable pageable);

    ProductAttribute save(Product product, String key, String value);

    Set<ProductAttribute> saveMany(Product product, Map<String, String> data);

    Set<ProductAttribute> setMany(Product product, Map<String, String> newAttributes);

    void deleteAttribute(ProductAttribute attribute);

    void deleteAttributeByName(String key);
}
