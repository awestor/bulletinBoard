package ru.daniil.bulletinBoard.service.category;

public interface CategoryProcessorService {

    void deleteWithReplace(String categoryName, String newCategoryName);

}
