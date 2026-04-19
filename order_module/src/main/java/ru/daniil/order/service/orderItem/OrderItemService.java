package ru.daniil.order.service.orderItem;

import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;

import java.util.List;

public interface OrderItemService {
    OrderItem createOrderItem(User user, Order order, Product product, Integer quantity);

    OrderItem getById(Long orderItemId);

    List<OrderItem> getByOrderNumber(String orderNumber);

    Integer countReservation(String sku);

    void updateItemQuantity(OrderItem item);

    void updatePriceAtTime(OrderItem item);

    boolean validateAvailability(Order order);

    void delete (Long orderItemId);
}
