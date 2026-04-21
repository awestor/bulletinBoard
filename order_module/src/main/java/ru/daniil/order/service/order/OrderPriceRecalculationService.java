package ru.daniil.order.service.order;

import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.entity.base.order.Order;

import java.math.BigDecimal;

public interface OrderPriceRecalculationService {

    void recalculateTotals(Order order);

    BigDecimal previewTotalWithNewDiscount(Order order, Discount newDiscount);
}
