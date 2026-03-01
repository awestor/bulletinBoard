package ru.daniil.bulletinBoard.entity.request;

import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CreateProductRequest {

    @NotBlank(message = "Название товара обязательно для заполнения")
    @Size(min = 3, max = 200, message = "Название товара должно содержать от 3 до 200 символов")
    private String name;

    @Size(max = 2000, message = "Описание товара не может превышать 2000 символов")
    private String description;

    @NotNull(message = "Цена товара обязательна для заполнения")
    @DecimalMin(value = "0.01", message = "Цена товара должна быть больше 0")
    @DecimalMax(value = "9999999.99", message = "Цена товара не может превышать 9 999 999.99")
    private BigDecimal price;

    @NotNull(message = "Категория товара обязательна для заполнения")
    @Min(value = 1, message = "ID категории должен быть положительным числом")
    private Long categoryId;

    @NotNull(message = "Количество на складе обязательно для заполнения")
    @Min(value = 0, message = "Количество на складе не может быть отрицательным")
    @Max(value = 999999, message = "Количество на складе не может превышать 999 999")
    private Integer stockQuantity;

    private List<MultipartFile> images;

    private Map<String, String> attributes;

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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public List<MultipartFile> getImages() {
        return images;
    }

    public void setImages(List<MultipartFile> images) {
        this.images = images;
    }
}
