package ru.daniil.product.productService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.enums.AuthProvider;
import ru.daniil.core.enums.CategoryType;
import ru.daniil.core.response.product.ProductFilterRequest;
import ru.daniil.product.repository.ProductRepository;
import ru.daniil.product.service.attribute.ProductAttributeService;
import ru.daniil.product.service.product.ProductServiceImpl;

import java.math.BigDecimal;
import java.util.*;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAttributeService attributeService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category("Test Category", CategoryType.LEAF);
        category.setId(1L);

        User user = new User("test@test.com", "testuser", "encodedPassword", AuthProvider.LOCAL);

        product = new Product(category, user, "Test Product", new BigDecimal("100.00"));
        product.setId(1L);
        product.setSku("SKU123");
        product.setStockQuantity(10);
        product.setDescription("Test Description");
    }

    @Test
    void save_ShouldReturnSavedProduct() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.save(product);

        assertNotNull(result);
        assertEquals(product, result);
        verify(productRepository).save(product);
    }

    @Test
    void generateSku_ShouldReturnValidSku() {
        String sku = productService.generateSku();

        assertNotNull(sku);
        assertEquals(12, sku.length());
        assertTrue(sku.matches("[A-Z0-9-]+"));
    }

    @Test
    void getById_WhenExists_ShouldReturnProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.getById(1L);

        assertEquals(product, result);
    }

    @Test
    void getById_WhenNotExists_ShouldThrowException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.getById(999L));

        assertEquals("Продукт не найден", exception.getMessage());
    }

    @Test
    void getBySku_ShouldReturnProduct() {
        when(productRepository.findBySku("SKU123")).thenReturn(product);

        Product result = productService.getBySku("SKU123");

        assertEquals(product, result);
    }

    @Test
    void getByCategory_ShouldReturnProducts() {
        List<Product> products = Collections.singletonList(product);
        when(productRepository.findByCategoryName("Test Category")).thenReturn(products);

        List<Product> result = productService.getByCategory("Test Category");

        assertEquals(1, result.size());
        assertEquals(product, result.getFirst());
    }

    @Test
    void getBySeller_ShouldReturnPage() {
        Pageable pageable = mock(Pageable.class);
        Page<Product> expectedPage = new PageImpl<>(Collections.singletonList(product));
        when(productRepository.findBySellerId(1L, pageable)).thenReturn(expectedPage);

        Page<Product> result = productService.getBySeller(1L, pageable);

        assertEquals(expectedPage, result);
        verify(productRepository).findBySellerId(1L, pageable);
    }

    @Test
    void filterProducts_WithNoFilters_ShouldReturnAll() {
        ProductFilterRequest filter = new ProductFilterRequest();
        Pageable pageable = mock(Pageable.class);
        Page<Product> expectedPage = new PageImpl<>(Collections.singletonList(product));
        when(productRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<Product> result = productService.filterProducts(filter, pageable);

        assertEquals(expectedPage, result);
        verify(productRepository).findAll(pageable);
    }

    @Test
    void filterProducts_WithFilters_ShouldReturnFiltered() {
        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setMinPrice(new BigDecimal("50.00"));
        filter.setMaxPrice(new BigDecimal("150.00"));
        filter.setInStock(true);
        filter.setNamePart("Test");
        Pageable pageable = mock(Pageable.class);
        Page<Product> expectedPage = new PageImpl<>(Collections.singletonList(product));

        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        Page<Product> result = productService.filterProducts(filter, pageable);

        assertEquals(expectedPage, result);
        verify(productRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void count_ShouldReturnTotalCount() {
        when(productRepository.count()).thenReturn(5L);

        long result = productService.count();

        assertEquals(5L, result);
        verify(productRepository).count();
    }

    @Test
    void updateStock_ShouldUpdateQuantity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.updateStock(1L, 15);

        assertEquals(15, product.getStockQuantity());
        verify(productRepository).save(product);
    }

    @Test
    void updateCategoryForProducts_ShouldUpdate() {
        List<Product> products = Collections.singletonList(product);

        productService.updateCategoryForProducts(products, category);

        verify(productRepository).updateCategoryForProducts(products, category);
    }

    @Test
    void delete_ShouldDeleteProduct() {
        productService.delete(product);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteAll_ShouldDeleteAllProducts() {
        productService.deleteAll();

        verify(productRepository).deleteAllInBatch();
    }
}