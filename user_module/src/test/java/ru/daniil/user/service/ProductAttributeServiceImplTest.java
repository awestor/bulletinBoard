package ru.daniil.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.product.ProductAttribute;
import ru.daniil.user.repository.product.ProductAttributeRepository;
import ru.daniil.user.service.product_module.product.attribute.ProductAttributeServiceImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductAttributeServiceImplTest {

    @Mock
    private ProductAttributeRepository productAttributeRepository;

    @InjectMocks
    private ProductAttributeServiceImpl attributeService;

    private Product product;
    private ProductAttribute attribute1;
    private ProductAttribute attribute2;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);

        attribute1 = new ProductAttribute(product, "color", "red");
        attribute1.setId(1L);

        attribute2 = new ProductAttribute(product, "size", "M");
        attribute2.setId(2L);

        Set<ProductAttribute> existingAttributes = new HashSet<>();
        existingAttributes.add(attribute1);
        existingAttributes.add(attribute2);
        product.setAttributes(existingAttributes);
    }

    @Test
    void save_ShouldCreateAndSaveAttribute() {
        when(productAttributeRepository.save(any(ProductAttribute.class))).thenAnswer(i -> i.getArgument(0));

        ProductAttribute result = attributeService.save(product, "newKey", "newValue");

        assertNotNull(result);
        assertEquals("newKey", result.getKey());
        assertEquals("newValue", result.getValue());
        assertEquals(product, result.getProduct());
        verify(productAttributeRepository).save(any(ProductAttribute.class));
    }

    @Test
    void saveMany_ShouldCreateMultipleAttributes() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("weight", "1kg");
        attributes.put("material", "cotton");

        when(productAttributeRepository.save(any(ProductAttribute.class)))
                .thenAnswer(i -> i.getArgument(0));

        Set<ProductAttribute> result = attributeService.saveMany(product, attributes);

        assertEquals(2, result.size());
        verify(productAttributeRepository, times(2)).save(any(ProductAttribute.class));
    }

    @Test
    void saveMany_WithEmptyMap_ShouldReturnEmptySet() {
        Set<ProductAttribute> result = attributeService.saveMany(product, new HashMap<>());

        assertTrue(result.isEmpty());
        verify(productAttributeRepository, never()).save(any());
    }

    @Test
    void setMany_WithAllNewAttributes_ShouldCreateNewAndRemoveOld() {
        Map<String, String> newAttributes = new HashMap<>();
        newAttributes.put("color", "blue");
        newAttributes.put("newKey", "newValue");

        when(productAttributeRepository.save(any(ProductAttribute.class)))
                .thenAnswer(i -> i.getArgument(0));
        doNothing().when(productAttributeRepository).delete(any(ProductAttribute.class));

        Set<ProductAttribute> result = attributeService.setMany(product, newAttributes);

        assertEquals(2, result.size());
        verify(productAttributeRepository, times(1)).save(any(ProductAttribute.class));
        verify(productAttributeRepository, times(1)).delete(any(ProductAttribute.class));
    }

    @Test
    void setMany_WithUpdatedAttributes_ShouldUpdateExisting() {
        Map<String, String> newAttributes = new HashMap<>();
        newAttributes.put("color", "blue");
        newAttributes.put("size", "L");

        Set<ProductAttribute> result = attributeService.setMany(product, newAttributes);

        assertEquals(2, result.size());

        ProductAttribute updatedColor = result.stream()
                .filter(a -> "color".equals(a.getKey()))
                .findFirst()
                .orElse(null);
        assertNotNull(updatedColor);
        assertEquals("blue", updatedColor.getValue());

        ProductAttribute updatedSize = result.stream()
                .filter(a -> "size".equals(a.getKey()))
                .findFirst()
                .orElse(null);
        assertNotNull(updatedSize);
        assertEquals("L", updatedSize.getValue());

        verify(productAttributeRepository, never()).save(any(ProductAttribute.class));
        verify(productAttributeRepository, never()).delete(any());
    }

    @Test
    void setMany_WithMixedAttributes_ShouldUpdateExistingCreateNewDeleteOld() {
        Map<String, String> newAttributes = new HashMap<>();
        newAttributes.put("color", "blue");
        newAttributes.put("weight", "2kg");

        when(productAttributeRepository.save(any(ProductAttribute.class)))
                .thenAnswer(i -> i.getArgument(0));
        doNothing().when(productAttributeRepository).delete(any(ProductAttribute.class));

        Set<ProductAttribute> result = attributeService.setMany(product, newAttributes);

        assertEquals(2, result.size());

        boolean hasColor = result.stream().anyMatch(a -> "color".equals(a.getKey()) && "blue".equals(a.getValue()));
        boolean hasWeight = result.stream().anyMatch(a -> "weight".equals(a.getKey()) && "2kg".equals(a.getValue()));
        boolean hasSize = result.stream().anyMatch(a -> "size".equals(a.getKey()));

        assertTrue(hasColor);
        assertTrue(hasWeight);
        assertFalse(hasSize);

        verify(productAttributeRepository, times(1)).save(any(ProductAttribute.class));
        verify(productAttributeRepository, times(1)).delete(attribute2);
    }

    @Test
    void setMany_WithEmptyMap_ShouldDeleteAll() {
        doNothing().when(productAttributeRepository).delete(any(ProductAttribute.class));

        Set<ProductAttribute> result = attributeService.setMany(product, new HashMap<>());

        assertTrue(result.isEmpty());
        verify(productAttributeRepository, times(2)).delete(any(ProductAttribute.class));
    }

    @Test
    void deleteAttribute_ShouldDelete() {
        attributeService.deleteAttribute(attribute1);

        verify(productAttributeRepository).delete(attribute1);
    }

    @Test
    void deleteAttributeByName_ShouldDeleteAllWithKey() {
        attributeService.deleteAttributeByName("color");

        verify(productAttributeRepository).deleteAttributeForProducts("color");
    }
}
