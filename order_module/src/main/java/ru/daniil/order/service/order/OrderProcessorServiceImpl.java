package ru.daniil.order.service.order;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.entity.base.discount.OrderDiscount;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.orderItem.CreateOrderItemRequest;
import ru.daniil.core.request.orderItem.DeleteOrderItemRequest;
import ru.daniil.core.request.orderItem.ReduceQuantityRequest;
import ru.daniil.order.service.orderItem.OrderItemService;
import ru.daniil.product.service.product.ProductService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderProcessorServiceImpl implements OrderProcessorService {
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final ProductService productService;

    public OrderProcessorServiceImpl(OrderService orderService,
                                     OrderItemService orderItemService,
                                     ProductService productService) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.productService = productService;
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
            recalculateTotals(order);
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
            recalculateTotals(order);
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
            recalculateTotals(order);
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

    private void recalculateTotals(Order order) {
        BigDecimal totalPrice = BigDecimal.ZERO;

        Map<Long, List<OrderDiscount>> discountsByCategory = order.getAppliedDiscounts().stream()
                .collect(Collectors.groupingBy(d -> d.getDiscount().getApplicableCategoryId()));

        for (OrderItem item : orderItemService.getByOrderNumber(order.getOrderNumber())) {
            Product product = item.getProduct();
            if (product == null) continue;

            BigDecimal priceAtTime = product.getPriceAtTime();
            BigDecimal priceWithoutDisc = priceAtTime.multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal maxDiscount = BigDecimal.ZERO;

            List<OrderDiscount> categoryDiscounts = discountsByCategory.get(product.getCategory().getId());
            if (categoryDiscounts != null) {
                for (OrderDiscount discount : categoryDiscounts) {
                    Discount discountObj = discount.getDiscount();

                    if (discountObj.getPercentage() != null) {
                        BigDecimal percentDisc = discountObj.getPercentage()
                                .multiply(priceWithoutDisc)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        maxDiscount = maxDiscount.max(percentDisc);
                    }

                    if (discountObj.getFixedAmount() != null) {
                        maxDiscount = maxDiscount.max(discountObj.getFixedAmount());
                    }
                }
            }

            BigDecimal discountedPrice = priceWithoutDisc.subtract(maxDiscount);
            totalPrice = totalPrice.add(discountedPrice);
            item.setPriceAtTime(discountedPrice);
            orderItemService.updatePriceAtTime(item);
        }

        order.setTotalPrice(totalPrice);
        orderService.updateTotalPrice(order);
    }
}
