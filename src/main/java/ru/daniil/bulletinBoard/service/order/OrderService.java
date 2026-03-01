package ru.daniil.bulletinBoard.service.order;

import ru.daniil.bulletinBoard.entity.base.order.Order;
import ru.daniil.bulletinBoard.entity.base.user.User;

import java.util.List;

public interface OrderService {
    Order create(User user);

    Order getById(Long id);

    Order getByOrderNumber(String orderNumber);

    Order getLastOrCreateOrderByUser(User user);

    void updateTotalPrice(Order order);

    List<Order> getByUser(User user);

    void delete (Long orderId);
}
