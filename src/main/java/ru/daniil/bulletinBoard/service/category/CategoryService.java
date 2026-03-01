package ru.daniil.bulletinBoard.service.category;

import ru.daniil.bulletinBoard.entity.base.product.Category;

public interface CategoryService {

    void delete(String categoryName);

    Category getByName(String categoryName);

    void updateParentForCategories(Category category, Category newCategory);
}
