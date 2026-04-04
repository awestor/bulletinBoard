package ru.daniil.core.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderDiscountResponse {
    private String discountCode;
    private String discountName;
    private String discountType;
    private BigDecimal discountAmount;
    private LocalDateTime reservedUntil;
}