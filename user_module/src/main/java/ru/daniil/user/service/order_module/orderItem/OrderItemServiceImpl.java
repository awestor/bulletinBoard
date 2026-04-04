package ru.daniil.user.service.order_module.orderItem;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.user.repository.order.OrderItemRepository;

import java.util.List;

@Service
public class OrderItemServiceImpl implements OrderItemService {
    private final OrderItemRepository orderItemRepository;

    public OrderItemServiceImpl(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    @Override
    public OrderItem createOrderItem(User user, Order order, Product product, Integer quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setPriceAtTime(product.getPriceAtTime());

        return orderItemRepository.save(orderItem);
    }

    @Override
    public OrderItem getById(Long orderItemId) {
        return orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("Указанный товар не найден в заказах"));
    }

    @Override
    public List<OrderItem> getByOrderNumber(String orderNumber) {
        return orderItemRepository.findByOrder_OrderNumber(orderNumber);
    }

    @Transactional
    @Override
    public void updateItemQuantity(OrderItem item) {
        if (item != null) {
            orderItemRepository.updateQuantity(item.getId(), item.getQuantity(), item.generateReservationTime());
        }
    }

    @Transactional
    @Override
    public void updatePriceAtTime(OrderItem item) {
        orderItemRepository.updatePrice(item.getId(), item.getPriceAtTime());
    }

    @Override
    public boolean validateAvailability(Order order){
        List<OrderItem> orderItems = orderItemRepository.findByOrder_OrderNumber(order.getOrderNumber());

        return orderItems.stream()
                .allMatch(item -> item.getProduct().getStockQuantity() >= item.getQuantity());
    }

    @Override
    public void delete(Long orderItemId) {
        OrderItem item = getById(orderItemId);
        orderItemRepository.delete(item);
    }
}
