package ru.daniil.bulletinBoard.service.category;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.daniil.bulletinBoard.entity.base.product.Category;
import ru.daniil.bulletinBoard.entity.request.CreateCategoryRequest;
import ru.daniil.bulletinBoard.enums.CategoryType;
import ru.daniil.bulletinBoard.repository.product.CategoryRepository;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private final CacheManager cacheManager;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CacheManager cacheManager) {
        this.categoryRepository = categoryRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    public Category create(CreateCategoryRequest request) {
        Category category;

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Категория родитель не найдена"));
            category = new Category(request.getName(), request.getType(), parent);
        } else {
            category = new Category(request.getName(), request.getType());
        }

        category.setDescription(request.getDescription());

        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category update(String categoryOldName, CreateCategoryRequest request) {
        Category category = getByName(categoryOldName);

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        if (request.getParentId() != null) {
            Category newParent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Родительская категория не найдена"));

            if (!newParent.equals(category.getParent())) {
                if (category.getParent() != null) {
                    category.getParent().removeChild(category);
                }
                newParent.addChild(category);
            }
        } else if (category.getParent() != null) {
            category.getParent().removeChild(category);
        }

        return categoryRepository.save(category);
    }

    @Override
    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Категория с указанным id не найдена"));
    }

    @Override
    public Category getByName(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new RuntimeException("Категория с указанным именем не найдена"));
    }

    @Override
    @Cacheable(
            value = "rootCat",
            key = "'rootCategories'",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    @Override
    public List<Category> getNextCategories(String categoryName) {
        Category category = getByName(categoryName);
        return categoryRepository.findByParentId(category.getId());
    }

    @Override
    @Cacheable(
            value = "leafCat",
            key = "'leafCategories'",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<Category> getLeafCategories() {
        return categoryRepository.findByType(CategoryType.LEAF);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "leafCat", key = "leafCategories"),
            @CacheEvict(value = "rootCat", key = "rootCategories")
    })
    public void delete(String categoryName) {
        Category category = getByName(categoryName);

        validateCategoryForDeletion(category);

        categoryRepository.delete(category);
    }

    @Override
    public void updateParentForCategories (Category oldCategory, Category newCategory){
        List<Category> children = getNextCategories(oldCategory.getName());
        categoryRepository.updateParentForCategories(children, newCategory);
    }

    private void validateCategoryForDeletion(Category category) {
        if (!category.isLeaf()) {
            throw new IllegalStateException(
                    String.format("Удаление категории '%s' невозможно: у категории есть потомки", category.getName())
            );
        }

        if (!category.getProducts().isEmpty()) {
            throw new IllegalStateException(
                    String.format("Удаление категории '%s' невозможно: к категории присвоены продукты", category.getName())
            );
        }
    }
}
