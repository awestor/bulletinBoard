package ru.daniil.product.service.category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.product.service.product.ProductService;


@Service
public class CategoryProcessorServiceImpl implements CategoryProcessorService {
    private final CategoryService categoryService;
    private final ProductService productService;

    public CategoryProcessorServiceImpl(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @Override
    @Transactional
    public void deleteWithReplace(String categoryName, String newCategoryName) {
        if (newCategoryName == null) {
            categoryService.delete(categoryName);
        } else {
            Category category = categoryService.getByName(categoryName);
            Category newCategory = categoryService.getByName(newCategoryName);
            transferChildrenAndProducts(category, newCategory);
            categoryService.delete(categoryName);
        }
    }

    private void transferChildrenAndProducts(Category category, Category newCategory) {
        if (!category.isLeaf()) {
            categoryService.updateParentForCategories(category, newCategory);
        } else if (!category.getProducts().isEmpty()) {
            transferProducts(category, newCategory);
        }
    }

    private void transferProducts(Category category, Category newCategory) {
        if (!newCategory.isLeaf()) {
            throw new IllegalStateException(
                    String.format("Категория '%s' не является конечной, присвоение ей продуктов невозможно",
                            newCategory.getName())
            );
        }

        productService.updateCategoryForProducts(category.getProducts(), newCategory);
    }
}
