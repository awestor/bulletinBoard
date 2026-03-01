package ru.daniil.bulletinBoard.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.daniil.bulletinBoard.enums.CategoryType;

public class CreateCategoryRequest {
    @NotBlank(message = "Название категории обязательно для заполнения")
    @Size(min = 2, max = 100, message = "Название категории должно содержать от 2 до 100 символов")
    private String name;

    @Size(max = 500, message = "Описание категории не может превышать 500 символов")
    private String description;

    @NotNull(message = "Тип категории обязателен для заполнения")
    private CategoryType type;

    private Long parentId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
