package ru.daniil.order.service.order;

import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.orderItem.CreateOrderItemRequest;
import ru.daniil.core.request.orderItem.DeleteOrderItemRequest;
import ru.daniil.core.request.orderItem.ReduceQuantityRequest;

public interface OrderProcessorService {
    OrderItem addOrderItem(CreateOrderItemRequest request, User user);

    void removeOrderItem(DeleteOrderItemRequest request, User user);

    void reduceQuantityOrderItem(ReduceQuantityRequest request, User user);
}
