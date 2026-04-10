package ru.daniil.product.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.product.ProductAttribute;
import ru.daniil.core.request.CreateProductRequest;
import ru.daniil.core.enums.CategoryType;
import ru.daniil.product.repository.ProductRepository;
import ru.daniil.product.service.attribute.ProductAttributeService;
import ru.daniil.image.service.product.ProductImageService;
import ru.daniil.product.service.product.ProductServiceImpl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageService productImageService;

    @Mock
    private ProductAttributeService attributeService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category leafCategory;
    private Category nonLeafCategory;
    private Product product;
    private CreateProductRequest request;

    @BeforeEach
    void setUp() {
        leafCategory = new Category("Leaf", CategoryType.LEAF);
        leafCategory.setId(1L);

        nonLeafCategory = new Category("Intermediate", CategoryType.INTERMEDIATE);
        nonLeafCategory.setId(2L);

        product = new Product(leafCategory, "Test Product", new BigDecimal("100.00"));
        product.setId(1L);
        product.setSku("SKU123");
        product.setStockQuantity(10);
        product.setDescription("Test Description");

        request = new CreateProductRequest();
        request.setName("New Product");
        request.setPrice(new BigDecimal("150.00"));
        request.setDescription("New Description");
        request.setStockQuantity(5);
        request.setCategoryId(1L);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("color", "red");
        attributes.put("size", "M");
        request.setAttributes(attributes);

        MultipartFile mockImage = mock(MultipartFile.class);
        request.setImages(Collections.singletonList(mockImage));
    }

    @Test
    void create_WithValidData_ShouldCreateProduct() throws IOException {
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        Set<ProductAttribute> attributes = new HashSet<>();
        when(attributeService.saveMany(any(Product.class), anyMap())).thenReturn(attributes);
        when(productImageService.saveImage(any(MultipartFile.class))).thenReturn("image.jpg");

        Product result = productService.create(request, leafCategory);

        assertNotNull(result);
        assertEquals("New Product", result.getName());
        assertEquals(new BigDecimal("150.00"), result.getPrice());
        assertEquals(leafCategory, result.getCategory());
        assertNotNull(result.getSku());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_WhenCategoryNotLeaf_ShouldThrowException() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.create(request, nonLeafCategory));

        assertEquals("Продукт не может быть размещён в не конечной категории", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_WithoutAttributes_ShouldCreateWithoutAttributes() throws IOException {
        request.setAttributes(null);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(productImageService.saveImage(any(MultipartFile.class))).thenReturn("image.jpg");

        Product result = productService.create(request, leafCategory);

        assertNotNull(result);
        verify(attributeService, never()).saveMany(any(), anyMap());
    }

    @Test
    void create_WithoutImages_ShouldCreateWithoutImages() throws IOException {
        request.setImages(null);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(attributeService.saveMany(any(Product.class), anyMap())).thenReturn(new HashSet<>());

        Product result = productService.create(request, leafCategory);

        assertNotNull(result);
        verify(productImageService, never()).saveImage(any());
    }

    @Test
    void create_WhenImageSaveFails_ShouldThrowException() throws IOException {
        when(productImageService.saveImage(any(MultipartFile.class))).thenThrow(new IOException("File error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.create(request, leafCategory));

        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void update_WithNewCategory_ShouldUpdateAllFields() {
        Category newCategory = new Category("New Leaf", CategoryType.LEAF);
        newCategory.setId(3L);
        request.setCategoryId(3L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        Set<ProductAttribute> attributes = new HashSet<>();
        when(attributeService.setMany(any(Product.class), anyMap())).thenReturn(attributes);

        Product result = productService.update(1L, request, newCategory);

        assertEquals("New Product", result.getName());
        assertEquals(new BigDecimal("150.00"), result.getPrice());
        assertEquals(newCategory, result.getCategory());
        verify(productRepository).save(product);
    }

    @Test
    void update_WhenNewCategoryNotLeaf_ShouldThrowException() {
        request.setCategoryId(2L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.update(1L, request, nonLeafCategory));

        assertEquals("Продукт не может быть назначен в не конечную категорию", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_WithoutCategoryChange_ShouldUpdateOtherFields() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        Set<ProductAttribute> attributes = new HashSet<>();
        when(attributeService.setMany(any(Product.class), anyMap())).thenReturn(attributes);

        Product result = productService.update(1L, request, leafCategory);

        assertEquals("New Product", result.getName());
        assertEquals(leafCategory, result.getCategory());
        verify(productRepository).save(product);
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
        when(productRepository.findByCategoryName("Leaf")).thenReturn(products);

        List<Product> result = productService.getByCategory("Leaf");

        assertEquals(1, result.size());
        assertEquals(product, result.getFirst());
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

        productService.updateCategoryForProducts(products, leafCategory);

        verify(productRepository).updateCategoryForProducts(products, leafCategory);
    }

    @Test
    void delete_ShouldDeleteProductAndAttributes() {
        Set<ProductAttribute> attributes = new HashSet<>();
        ProductAttribute attribute = mock(ProductAttribute.class);
        attributes.add(attribute);
        product.setAttributes(attributes);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).delete(product);

        productService.delete(1L);

        verify(attributeService).deleteAttribute(attribute);
        verify(productRepository).delete(product);
    }
}