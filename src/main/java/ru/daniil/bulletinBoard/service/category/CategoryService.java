package ru.daniil.bulletinBoard.service.category;

import ru.daniil.bulletinBoard.entity.base.product.Category;
import ru.daniil.bulletinBoard.entity.request.CreateCategoryRequest;

import java.util.List;

public interface CategoryService {

    Category create(CreateCategoryRequest request);

    void delete(String categoryName);

    Category update(String categoryOldName, CreateCategoryRequest request);

    Category getByName(String categoryName);

    Category getById(Long id);

    List<Category> getRootCategories();

    List<Category> getLeafCategories();

    List<Category> getNextCategories(String categoryName);

    void updateParentForCategories(Category category, Category newCategory);
}
