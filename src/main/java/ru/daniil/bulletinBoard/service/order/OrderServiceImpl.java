package ru.daniil.bulletinBoard.service.order;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.daniil.bulletinBoard.entity.base.order.Order;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.enums.OrderStatus;
import ru.daniil.bulletinBoard.repository.order.OrderRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService{
    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    @Override
    public Order create(User user) {
        if (user.isTradingBlocked()) {
            throw new RuntimeException("Торговля заблокирована для данного пользователя");
        }

        Order order = new Order(user);
        return orderRepository.save(order);
    }

    @Override
    public Order getById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    @Override
    public Order getByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    @Transactional
    @Override
    public Order getLastOrCreateOrderByUser(User user) {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Order> lastOrders = orderRepository.findLastByUserId(
                user.getId(),
                pageable
        );

        Optional<Order> lastOrder = lastOrders.stream().findFirst();

        if (lastOrder.isEmpty()) {
            return create(user);
        }
        if (Objects.equals(lastOrder.get().getStatus(), OrderStatus.PAID)) {
            return create(user);
        }

        return lastOrder.get();
    }

    @Override
    public List<Order> getByUser(User user) {
        return orderRepository.findByUserId(user.getId());
    }

    @Override
    public void updateTotalPrice(Order order){
        orderRepository.updateTotalPrice(order.getId(), order.getTotalPrice());
    }

    @Transactional
    @Override
    public void delete(Long orderId) {
        Order order = getById(orderId);

        if (!order.getStatus().equals(OrderStatus.DRAFT)) {
            throw new RuntimeException("Завершённые заказы невозможно удалить");
        }

        orderRepository.delete(order);
    }
}
