package ru.daniil.user.service.product_module.product.attribute;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.product.ProductAttribute;
import ru.daniil.user.repository.product.ProductAttributeRepository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductAttributeServiceImpl implements ProductAttributeService {

    private final ProductAttributeRepository productAttributeRepository;

    public ProductAttributeServiceImpl(ProductAttributeRepository productAttributeRepository) {
        this.productAttributeRepository = productAttributeRepository;
    }

    @Override
    public ProductAttribute save(Product product, String key, String value) {
         ProductAttribute attribute = new ProductAttribute(
                 product,
                 key,
                 value
         );
        productAttributeRepository.save(attribute);
         return attribute;
    }

    @Override
    public Set<ProductAttribute> saveMany(Product product, Map<String, String> data) {
        Set<ProductAttribute> attributes = new HashSet<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            attributes.add(save(product, entry.getKey(), entry.getValue()));
        }
        return attributes;
    }

    @Override
    @Transactional
    public Set<ProductAttribute> setMany(Product product, Map<String, String> newAttributes) {
        Set<ProductAttribute> oldAttributes = product.getAttributes();

        Map<String, ProductAttribute> existingAttributesMap = oldAttributes.stream()
                .collect(Collectors.toMap(ProductAttribute::getKey, attr -> attr));

        Set<ProductAttribute> updatedAttributes = new HashSet<>();

        for (Map.Entry<String, String> entry : newAttributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            ProductAttribute attribute = existingAttributesMap.get(key);

            if (attribute != null) {
                attribute.setValue(value);
                updatedAttributes.add(attribute);
                existingAttributesMap.remove(key);
            } else {
                ProductAttribute newAttribute = new ProductAttribute(product, key, value);
                productAttributeRepository.save(newAttribute);
                updatedAttributes.add(newAttribute);
            }
        }

        for (ProductAttribute attributeToDelete : existingAttributesMap.values()) {
            productAttributeRepository.delete(attributeToDelete);
            oldAttributes.remove(attributeToDelete);
        }

        product.setAttributes(updatedAttributes);

        return updatedAttributes;
    }

    @Override
    public void deleteAttribute(ProductAttribute attribute) {
        productAttributeRepository.delete(attribute);
    }

    @Override
    public void deleteAttributeByName(String key) {
        productAttributeRepository.deleteAttributeForProducts(key);
    }
}
