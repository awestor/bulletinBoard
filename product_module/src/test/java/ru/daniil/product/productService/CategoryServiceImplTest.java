package ru.daniil.product.productService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.enums.CategoryType;
import ru.daniil.core.request.CreateCategoryRequest;
import ru.daniil.product.repository.CategoryRepository;
import ru.daniil.product.service.category.CategoryServiceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category parentCategory;
    private Category childCategory;
    private CreateCategoryRequest request;

    @BeforeEach
    void setUp() {
        parentCategory = new Category("Parent", CategoryType.INTERMEDIATE);
        parentCategory.setId(1L);
        parentCategory.setDescription("Parent Description");

        childCategory = new Category("Child", CategoryType.LEAF, parentCategory);
        childCategory.setId(2L);
        childCategory.setDescription("Child Description");

        request = new CreateCategoryRequest();
        request.setName("New Category");
        request.setType(CategoryType.LEAF);
        request.setDescription("New Description");
        request.setParentId(1L);
    }

    @Test
    void create_WithParent_ShouldCreateAndSetParent() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        Category result = categoryService.create(request);

        assertNotNull(result);
        assertEquals("New Category", result.getName());
        assertEquals(CategoryType.LEAF, result.getType());
        assertEquals(parentCategory, result.getParent());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_WithoutParent_ShouldCreateRootCategory() {
        request.setParentId(null);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        Category result = categoryService.create(request);

        assertNotNull(result);
        assertNull(result.getParent());
        verify(categoryRepository).save(any(Category.class));
        verify(categoryRepository, never()).findById(anyLong());
    }

    @Test
    void create_WhenParentNotFound_ShouldThrowException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> categoryService.create(request));

        assertEquals("Категория родитель не найдена", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void update_ShouldUpdateCategoryFields() {
        String oldName = "Old Category";
        when(categoryRepository.findByName(oldName)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(childCategory);

        Category result = categoryService.update(oldName, request);

        assertEquals("New Category", result.getName());
        assertEquals("New Description", result.getDescription());
        assertEquals(parentCategory, result.getParent());
        verify(categoryRepository).save(childCategory);
    }

    @Test
    void update_WhenRemovingParent_ShouldDetachFromParent() {
        String oldName = "Old Category";
        request.setParentId(null);

        when(categoryRepository.findByName(oldName)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(childCategory);

        Category result = categoryService.update(oldName, request);

        assertNull(result.getParent());
        verify(categoryRepository).save(childCategory);
    }

    @Test
    void getById_WhenFound_ShouldReturnCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));

        Category result = categoryService.getById(1L);

        assertEquals(parentCategory, result);
    }

    @Test
    void getById_WhenNotFound_ShouldThrowException() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> categoryService.getById(999L));

        assertEquals("Категория с указанным id не найдена", exception.getMessage());
    }

    @Test
    void getByName_WhenFound_ShouldReturnCategory() {
        when(categoryRepository.findByName("Parent")).thenReturn(Optional.of(parentCategory));

        Category result = categoryService.getByName("Parent");

        assertEquals(parentCategory, result);
    }

    @Test
    void getRootCategories_ShouldReturnRootCategories() {
        List<Category> rootCategories = Collections.singletonList(parentCategory);
        when(categoryRepository.findByType(CategoryType.ROOT)).thenReturn(rootCategories);

        List<Category> result = categoryService.getRootCategories();

        assertEquals(1, result.size());
        assertEquals(parentCategory, result.getFirst());
    }

    @Test
    void getNextCategories_ShouldReturnChildren() {
        List<Category> children = Collections.singletonList(childCategory);
        when(categoryRepository.findByName("Parent")).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.findByParentId(1L)).thenReturn(children);

        List<Category> result = categoryService.getNextCategories("Parent");

        assertEquals(1, result.size());
        assertEquals(childCategory, result.getFirst());
    }

    @Test
    void delete_WhenCategoryIsLeafAndHasNoProducts_ShouldDelete() {
        childCategory.setProducts(List.of());
        when(categoryRepository.findByName("Child")).thenReturn(Optional.of(childCategory));
        doNothing().when(categoryRepository).delete(childCategory);

        categoryService.delete("Child");

        verify(categoryRepository).delete(childCategory);
    }

    @Test
    void delete_WhenCategoryHasChildren_ShouldThrowException() {
        List<Category> children = Collections.singletonList(childCategory);
        when(categoryRepository.findByName("Parent")).thenReturn(Optional.of(parentCategory));
        when(categoryService.getNextCategories(parentCategory.getName())).thenReturn(children);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> categoryService.delete("Parent"));

        assertTrue(exception.getMessage().contains("у категории есть потомки"));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_WhenCategoryHasProducts_ShouldThrowException() {
        childCategory.setProducts(Collections.singletonList(mock(Product.class)));
        when(categoryRepository.findByName("Child")).thenReturn(Optional.of(childCategory));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> categoryService.delete("Child"));

        assertTrue(exception.getMessage().contains("присвоены продукты"));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void updateParentForCategories_ShouldUpdateParent() {
        List<Category> children = Collections.singletonList(childCategory);
        when(categoryRepository.findByName("Parent")).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.findByParentId(parentCategory.getId())).thenReturn(children);

        categoryService.updateParentForCategories(parentCategory, parentCategory);

        verify(categoryRepository).updateParentForCategories(children, parentCategory);
    }
}
