package ru.daniil.discount.service;

import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.request.discount.CreateDiscountRequest;
import ru.daniil.core.sharedInterfaces.DiscountProvider;

import java.time.LocalDate;
import java.util.List;

public interface DiscountService extends DiscountProvider {

    List<Discount> getAllActiveDiscount();

    Discount getDiscountByCode(String code);

    Discount createDiscount(CreateDiscountRequest request);

    boolean isAvailable(Discount discount);

    void activateDiscount(String code, Integer usages);

    void deactivateDiscount(String code);
}
