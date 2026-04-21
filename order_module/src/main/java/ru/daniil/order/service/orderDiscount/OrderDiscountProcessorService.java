package ru.daniil.order.service.orderDiscount;

import ru.daniil.core.entity.base.discount.OrderDiscount;
import ru.daniil.core.entity.base.user.User;

import java.util.List;

public interface OrderDiscountProcessorService {

    OrderDiscount applyDiscountToOrder(
            String orderNumber, String discountCode, User user);

    void removeDiscountFromOrder(
            String orderNumber, String discountCode, User user);

    List<OrderDiscount> getAppliedDiscounts(String orderNumber, User user);
}
