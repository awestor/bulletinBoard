package ru.daniil.order.service.orderItem;

import ru.daniil.core.entity.base.discount.OrderDiscount;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.response.orderItem.OrderItemsCalculationResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OrderItemService {
    OrderItem createOrderItem(User user, Order order,
                              Product product, Integer quantity);

    OrderItem getById(Long orderItemId);

    List<OrderItem> getByOrderNumber(String orderNumber);

    Integer countReservation(String sku);

    void updateItemQuantity(OrderItem item);

    void updatePriceAtTime(OrderItem item);

    boolean validateAvailability(Order order);

    void delete (Long orderItemId);

    OrderItemsCalculationResult calculateOrderItemsPrices(
            Order order,
            Map<Long, List<OrderDiscount>> discountsByCategory);

    void applyCalculatedPrices(OrderItemsCalculationResult calculationResult);
}
