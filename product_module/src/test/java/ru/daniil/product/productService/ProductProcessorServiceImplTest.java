package ru.daniil.product.productService;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
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
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.enums.AuthProvider;
import ru.daniil.core.enums.CategoryType;
import ru.daniil.core.exceptions.UserBlockedExeption;
import ru.daniil.core.request.CreateProductRequest;
import ru.daniil.image.service.product.ProductImageService;
import ru.daniil.product.service.attribute.ProductAttributeService;
import ru.daniil.product.service.category.CategoryService;
import ru.daniil.product.service.product.ProductService;
import ru.daniil.product.service.product.ProductProcessorServiceImpl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductProcessorServiceImplTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductImageService productImageService;

    @Mock
    private ProductAttributeService attributeService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductProcessorServiceImpl productProcessorService;

    private Category leafCategory;
    private Category nonLeafCategory;
    private Product product;
    private CreateProductRequest request;
    private User user;

    @BeforeEach
    void setUp() {
        leafCategory = new Category("Leaf", CategoryType.LEAF);
        leafCategory.setId(1L);

        nonLeafCategory = new Category("Intermediate", CategoryType.INTERMEDIATE);
        nonLeafCategory.setId(2L);

        user = new User("test@test.com", "testuser", "encodedPassword", AuthProvider.LOCAL);
        user.setId(1L);
        user.setTradingBlocked(false);

        product = new Product(leafCategory, user, "Test Product", new BigDecimal("100.00"));
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
        when(categoryService.getById(1L)).thenReturn(leafCategory);
        when(productService.generateSku()).thenReturn("NEWSKU123456");
        when(productService.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(attributeService.saveMany(any(Product.class), anyMap())).thenReturn(new HashSet<>());
        when(productImageService.saveImage(any(MultipartFile.class))).thenReturn("image.jpg");
        doNothing().when(productImageService).save(any());

        Product result = productProcessorService.create(request, user);

        assertNotNull(result);
        assertEquals("New Product", result.getName());
        assertEquals(new BigDecimal("150.00"), result.getPrice());
        assertEquals(leafCategory, result.getCategory());
        assertEquals(user, result.getSeller());
        assertNotNull(result.getSku());
        verify(productService).save(any(Product.class));
        verify(attributeService).saveMany(any(Product.class), anyMap());
        verify(productImageService, times(1)).saveImage(any(MultipartFile.class));
    }

    @Test
    void create_WhenUserTradingBlocked_ShouldThrowException() {
        user.setTradingBlocked(true);

        UserBlockedExeption exception = assertThrows(UserBlockedExeption.class,
                () -> productProcessorService.create(request, user));

        assertEquals("Пользователю запрещено выставлять объявления о продаже", exception.getMessage());
        verify(productService, never()).save(any());
    }

    @Test
    void create_WhenCategoryNotLeaf_ShouldThrowException() {
        when(categoryService.getById(2L)).thenReturn(nonLeafCategory);
        request.setCategoryId(2L);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> productProcessorService.create(request, user));

        assertEquals("Продукт не может быть размещён в не конечной категории", exception.getMessage());
        verify(productService, never()).save(any());
    }

    @Test
    void create_WithoutAttributes_ShouldCreateWithoutAttributes() throws IOException {
        request.setAttributes(null);
        when(categoryService.getById(1L)).thenReturn(leafCategory);
        when(productService.generateSku()).thenReturn("NEWSKU123456");
        when(productService.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        Product result = productProcessorService.create(request, user);

        assertNotNull(result);
        verify(attributeService, never()).saveMany(any(), anyMap());
    }

    @Test
    void create_WithoutImages_ShouldCreateWithoutImages() throws IOException {
        request.setImages(null);
        when(categoryService.getById(1L)).thenReturn(leafCategory);
        when(productService.generateSku()).thenReturn("NEWSKU123456");
        when(productService.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(attributeService.saveMany(any(Product.class), anyMap())).thenReturn(new HashSet<>());

        Product result = productProcessorService.create(request, user);

        assertNotNull(result);
        verify(productImageService, never()).saveImage(any());
        verify(productImageService, never()).save(any());
    }

    @Test
    void create_WhenImageSaveFails_ShouldThrowException() {
        when(categoryService.getById(1L)).thenReturn(leafCategory);
        when(productService.generateSku()).thenReturn("NEWSKU123456");
        when(productService.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(productImageService.saveImage(any(MultipartFile.class))).thenThrow(new RuntimeException("Ошибка сохранения изображения"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productProcessorService.create(request, user));

        assertEquals("Продукт не был создан. Причина: Ошибка сохранения изображения",
                exception.getMessage());
    }

    @Test
    void update_WithNewCategory_ShouldUpdateAllFields() {
        Category newCategory = new Category("New Leaf", CategoryType.LEAF);
        newCategory.setId(3L);
        request.setCategoryId(3L);

        when(productService.getById(1L)).thenReturn(product);
        when(productService.save(any(Product.class))).thenReturn(product);
        when(attributeService.setMany(any(Product.class), anyMap())).thenReturn(new HashSet<>());

        Product result = productProcessorService.update(1L, request, newCategory);

        assertEquals("New Product", result.getName());
        assertEquals(new BigDecimal("150.00"), result.getPrice());
        assertEquals(newCategory, result.getCategory());
        verify(productService).save(product);
    }

    @Test
    void update_WhenNewCategoryNotLeaf_ShouldThrowException() {
        request.setCategoryId(2L);
        when(productService.getById(1L)).thenReturn(product);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productProcessorService.update(1L, request, nonLeafCategory));

        assertEquals("Продукт не может быть назначен в не конечную категорию", exception.getMessage());
        verify(productService, never()).save(any());
    }

    @Test
    void update_WithoutCategoryChange_ShouldUpdateOtherFields() {
        when(productService.getById(1L)).thenReturn(product);
        when(productService.save(any(Product.class))).thenReturn(product);
        when(attributeService.setMany(any(Product.class), anyMap())).thenReturn(new HashSet<>());

        Product result = productProcessorService.update(1L, request, leafCategory);

        assertEquals("New Product", result.getName());
        assertEquals(leafCategory, result.getCategory());
        verify(productService).save(product);
    }

    @Test
    void update_WhenProductNotFound_ShouldThrowNotFoundException() {
        when(productService.getById(1L)).thenThrow(new NotFoundException("Продукт не найден"));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> productProcessorService.update(1L, request, leafCategory));

        assertEquals("Продукт для редактирования не был найден", exception.getMessage());
        verify(productService, never()).save(any());
    }

    @Test
    void delete_ShouldDeleteProductAndAttributesAndImages() {
        Set<ProductAttribute> attributes = new HashSet<>();
        ProductAttribute attribute = mock(ProductAttribute.class);
        attributes.add(attribute);
        product.setAttributes(attributes);

        when(productService.getById(1L)).thenReturn(product);
        doNothing().when(productImageService).deleteAllImages(product.getId());
        doNothing().when(attributeService).deleteAttribute(attribute);
        doNothing().when(productService).delete(product);

        productProcessorService.delete(1L);

        verify(productImageService).deleteAllImages(1L);
        verify(attributeService).deleteAttribute(attribute);
        verify(productService).delete(product);
    }

    @Test
    void deleteAll_ShouldDeleteAllProductsImagesAndAttributes() {
        productProcessorService.deleteAll();

        verify(productImageService).deleteAllinButch();
        verify(productService).deleteAll();
    }
}