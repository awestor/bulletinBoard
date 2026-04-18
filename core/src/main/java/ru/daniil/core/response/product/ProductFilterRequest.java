package ru.daniil.core.response.product;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;

@Data
public class ProductFilterRequest {

    private Boolean inStock = true;

    @DecimalMin(value = "0.0", message = "Минимальная цена не может быть отрицательной")
    @Digits(integer = 10, fraction = 2, message = "Цена должна быть в формате до 10 целых и 2 знаков после запятой")
    private BigDecimal minPrice = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Максимальная цена не может быть отрицательной")
    @DecimalMax(value = "1000000.0", message = "Максимальная цена не может быть выше порога")
    @Digits(integer = 10, fraction = 2, message = "Цена должна быть в формате до 10 целых и 2 знаков после запятой")
    private BigDecimal maxPrice = new BigDecimal("1000000");

    private String namePart;

    private String skuPart;

    private String categoryName;

    private String sellerLogin;

    /**
     * Корректирует диапазон цен, чтобы minPrice не был больше maxPrice
     */
    public void normalizePriceRange() {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            maxPrice = minPrice;
        }
    }

    /**
     * Проверяет, есть ли хоть один фильтр
     * @return наличие хотя бы одного фильтра
     */
    public boolean hasAnyFilter() {
        return inStock != true ||
                !Objects.equals(minPrice, BigDecimal.ZERO) ||
                !Objects.equals(maxPrice, new BigDecimal("1000000")) ||
                (namePart != null && !namePart.isBlank()) ||
                (skuPart != null && !skuPart.isBlank()) ||
                (categoryName != null && !categoryName.isBlank()) ||
                (sellerLogin != null && !sellerLogin.isBlank());
    }
}