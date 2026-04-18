package ru.daniil.core.response.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductImageResponse {
    private Long id;
    private Long productId;
    private String fullPath;
    private Boolean isMain;
}