package ru.daniil.core.response.product;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProductResponse {

    private String name;

    private String description;

    private BigDecimal price;

    private BigDecimal priceAtTime;

    private String sku;

    private Integer stockQuantity;

    private String status;

    private String categoryName;
}
