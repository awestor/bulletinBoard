package ru.daniil.product.service.category;

public interface CategoryProcessorService {

    void deleteWithReplace(String categoryName, String newCategoryName);

}
