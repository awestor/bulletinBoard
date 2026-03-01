package ru.daniil.bulletinBoard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.daniil.bulletinBoard.entity.base.product.Category;
import ru.daniil.bulletinBoard.entity.base.product.Product;
import ru.daniil.bulletinBoard.enums.CategoryType;
import ru.daniil.bulletinBoard.service.category.CategoryProcessorServiceImpl;
import ru.daniil.bulletinBoard.service.category.CategoryService;
import ru.daniil.bulletinBoard.service.product.ProductService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryProcessorServiceImplTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CategoryProcessorServiceImpl categoryProcessorService;

    private Category leafCategory;
    private Category intermediateCategory;
    private Category newCategory;
    private List<Category> children;

    @BeforeEach
    void setUp() {
        leafCategory = new Category("Leaf Category", CategoryType.LEAF);
        leafCategory.setId(1L);

        intermediateCategory = new Category("Intermediate Category", CategoryType.INTERMEDIATE);
        intermediateCategory.setId(2L);

        newCategory = new Category("New Category", CategoryType.LEAF);
        newCategory.setId(3L);

        children = Arrays.asList(
                new Category("Child 1", CategoryType.LEAF),
                new Category("Child 2", CategoryType.LEAF)
        );
    }

    @Test
    void deleteWithReplace_WhenNewCategoryIsNull_ShouldDeleteOnly() {
        String categoryName = "Test Category";

        categoryProcessorService.deleteWithReplace(categoryName, null);

        verify(categoryService, times(1)).delete(categoryName);
        verify(categoryService, never()).getByName(anyString());
        verify(productService, never()).updateCategoryForProducts(anyList(), any());
    }

    @Test
    void deleteWithReplace_WhenCategoryIsIntermediate_ShouldTransferChildren() {
        String categoryName = "Intermediate Category";
        String newCategoryName = "New Category";

        when(categoryService.getByName(categoryName)).thenReturn(intermediateCategory);
        when(categoryService.getByName(newCategoryName)).thenReturn(newCategory);

        categoryProcessorService.deleteWithReplace(categoryName, newCategoryName);

        verify(categoryService).updateParentForCategories(intermediateCategory, newCategory);
        verify(categoryService).delete(categoryName);
        verify(productService, never()).updateCategoryForProducts(anyList(), any());
    }

    @Test
    void deleteWithReplace_WhenCategoryIsLeafWithProducts_ShouldTransferProducts() {
        String categoryName = children.getFirst().getName();
        String newCategoryName = "New Category";

        List<ru.daniil.bulletinBoard.entity.base.product.Product> products =
                Collections.singletonList(mock(Product.class));

        leafCategory.setProducts(products);

        when(categoryService.getByName(categoryName)).thenReturn(leafCategory);
        when(categoryService.getByName(newCategoryName)).thenReturn(newCategory);

        categoryProcessorService.deleteWithReplace(categoryName, newCategoryName);

        verify(productService).updateCategoryForProducts(products, newCategory);
        verify(categoryService).delete(categoryName);
        verify(categoryService, never()).updateParentForCategories(any(), any());
    }

    @Test
    void deleteWithReplace_WhenCategoryIsLeafWithoutProducts_ShouldDeleteOnly() {
        String categoryName = children.getFirst().getName();;
        String newCategoryName = "New Category";

        leafCategory.setProducts(new ArrayList<>());

        when(categoryService.getByName(categoryName)).thenReturn(leafCategory);
        when(categoryService.getByName(newCategoryName)).thenReturn(newCategory);

        categoryProcessorService.deleteWithReplace(categoryName, newCategoryName);

        verify(categoryService).delete(categoryName);
        verify(productService, never()).updateCategoryForProducts(anyList(), any());
        verify(categoryService, never()).updateParentForCategories(any(), any());
    }

    @Test
    void transferProducts_WhenNewCategoryIsNotLeaf_ShouldThrowException() {
        String categoryName = children.getFirst().getName();;
        String newCategoryName = "Intermediate Category";

        Category nonLeafNewCategory = new Category("Intermediate", CategoryType.INTERMEDIATE);
        List<ru.daniil.bulletinBoard.entity.base.product.Product> products =
                Collections.singletonList(mock(Product.class));

        leafCategory.setProducts(products);

        when(categoryService.getByName(categoryName)).thenReturn(leafCategory);
        when(categoryService.getByName(newCategoryName)).thenReturn(nonLeafNewCategory);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> categoryProcessorService.deleteWithReplace(categoryName, newCategoryName));

        assertTrue(exception.getMessage().contains("не является конечной"));
        verify(categoryService, never()).delete(anyString());
        verify(productService, never()).updateCategoryForProducts(anyList(), any());
    }
}