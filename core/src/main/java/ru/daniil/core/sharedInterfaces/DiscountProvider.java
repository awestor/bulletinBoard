package ru.daniil.core.sharedInterfaces;

import ru.daniil.core.entity.base.discount.Discount;

public interface DiscountProvider {

    Discount getActiveDiscountByCode(String code);

    void incrementUsageCount(Long discountId);

    void decrementUsageCount(Long discountId);
}
