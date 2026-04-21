package ru.daniil.core.response.discount;

import lombok.Builder;
import lombok.Data;
import ru.daniil.core.enums.DiscountType;

import java.math.BigDecimal;

@Data
@Builder
public class DiscountInfo {
    private String discountCode;
    private DiscountType type;
    private BigDecimal percentage;
    private BigDecimal fixedAmount;
    private Long applicableCategoryId;
}
