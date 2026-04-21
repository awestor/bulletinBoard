package ru.daniil.order.service.orderDiscount;

import ru.daniil.core.entity.base.discount.OrderDiscount;

import java.util.List;
import java.util.Optional;

public interface OrderDiscountService {
    List<OrderDiscount> getOrderDiscountByOrderId(Long orderId);

    OrderDiscount save(OrderDiscount orderDiscount);

    Optional<OrderDiscount> getOrderDiscountByIds(Long orderId, Long discountId);

    void deleteByIds(Long orderId, Long discountId);
}
