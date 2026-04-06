package ru.daniil.order.service.order;

import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.request.CreateOrderItemRequest;
import ru.daniil.core.request.DeleteOrderItemRequest;

public interface OrderProcessorService {
    OrderItem addOrderItem(CreateOrderItemRequest request);

    void removeOrderItem(DeleteOrderItemRequest request);
}
