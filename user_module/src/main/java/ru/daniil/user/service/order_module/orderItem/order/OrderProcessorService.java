package ru.daniil.user.service.order_module.orderItem.order;

import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.request.CreateOrderItemRequest;
import ru.daniil.core.request.DeleteOrderItemRequest;

public interface OrderProcessorService {
    OrderItem addOrderItem(CreateOrderItemRequest request);

    void removeOrderItem(DeleteOrderItemRequest request);
}
