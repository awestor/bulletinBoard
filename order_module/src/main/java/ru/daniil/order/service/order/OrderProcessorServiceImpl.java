package ru.daniil.order.service.order;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.orderItem.CreateOrderItemRequest;
import ru.daniil.core.request.orderItem.DeleteOrderItemRequest;
import ru.daniil.core.request.orderItem.ReduceQuantityRequest;
import ru.daniil.order.service.orderItem.OrderItemService;
import ru.daniil.product.service.product.ProductService;

import java.util.List;
import java.util.Optional;

@Service
public class OrderProcessorServiceImpl implements OrderProcessorService {
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final ProductService productService;
    private final OrderPriceRecalculationServiceImpl orderPriceRecalculationServiceImpl;

    public OrderProcessorServiceImpl(OrderService orderService,
                                     OrderItemService orderItemService,
                                     ProductService productService,
                                     OrderPriceRecalculationServiceImpl orderPriceRecalculationServiceImpl) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.productService = productService;
        this.orderPriceRecalculationServiceImpl = orderPriceRecalculationServiceImpl;
    }

    @Transactional
    @Override
    public OrderItem addOrderItem(CreateOrderItemRequest request, User user) {
        Order order = orderService.getLastOrCreateOrderByUser(user);
        Product product = productService.getBySku(request.getSku());
        Integer reservationCount = orderItemService.countReservation(request.getSku());

        if (product.getStockQuantity() >= (request.getQuantity() + reservationCount)){
            Optional<OrderItem> existingItem = itemExist(order, request.getSku());

            OrderItem item;
            if (existingItem.isPresent()) {
                item = existingItem.get();
                item.setQuantity(item.getQuantity() + request.getQuantity());
                orderItemService.updateItemQuantity(item);
            } else {
                item = orderItemService.createOrderItem(user, order, product, request.getQuantity());
            }
            orderPriceRecalculationServiceImpl.recalculateTotals(order);
            return item;
        }
        else throw new IllegalArgumentException
                ("Указанное количество покупаемого продукта превышает его количество в наличии");
    }

    @Transactional
    @Override
    public void removeOrderItem(DeleteOrderItemRequest request, User user) {
        Order order = orderService.getLastOrCreateOrderByUser(user);

        Optional<OrderItem> existingItem = itemExist(order, request.getSku());

        if (existingItem.isPresent()){
            orderItemService.delete(existingItem.get().getId());
            orderPriceRecalculationServiceImpl.recalculateTotals(order);
        }
        else {
            throw new EntityNotFoundException("Заказанный предмет с указанным номером не найден");
        }
    }

    @Transactional
    @Override
    public void reduceQuantityOrderItem(ReduceQuantityRequest request, User user) {
        Order order = orderService.getLastOrCreateOrderByUser(user);

        Optional<OrderItem> existingItem = itemExist(order, request.getSku());

        if (existingItem.isPresent()){
            OrderItem item = existingItem.get();
            item.setQuantity(item.getQuantity() - request.getQuantity());
            orderItemService.updateItemQuantity(item);
            orderPriceRecalculationServiceImpl.recalculateTotals(order);
        }
        else {
            throw new EntityNotFoundException("Заказанный предмет с указанным номером не найден");
        }
    }

    private Optional<OrderItem> itemExist(Order order, String sku){
        List<OrderItem> existingItems = orderItemService.getByOrderNumber(order.getOrderNumber());
        return existingItems.stream()
                .filter(item -> item.getProduct().getSku().equals(sku))
                .findFirst();
    }
}
