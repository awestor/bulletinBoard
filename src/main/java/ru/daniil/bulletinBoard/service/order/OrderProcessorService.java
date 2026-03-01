package ru.daniil.bulletinBoard.service.order;

import ru.daniil.bulletinBoard.entity.base.order.OrderItem;
import ru.daniil.bulletinBoard.entity.request.CreateOrderItemRequest;
import ru.daniil.bulletinBoard.entity.request.DeleteOrderItemRequest;

public interface OrderProcessorService {
    OrderItem addOrderItem(CreateOrderItemRequest request);

    void removeOrderItem(DeleteOrderItemRequest request);
}
