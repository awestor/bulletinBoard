package ru.daniil.user.service.product_module.product.category;

public interface CategoryProcessorService {

    void deleteWithReplace(String categoryName, String newCategoryName);

}
