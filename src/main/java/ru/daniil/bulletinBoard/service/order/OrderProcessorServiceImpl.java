package ru.daniil.bulletinBoard.service.order;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.daniil.bulletinBoard.entity.base.discount.Discount;
import ru.daniil.bulletinBoard.entity.base.discount.OrderDiscount;
import ru.daniil.bulletinBoard.entity.base.order.Order;
import ru.daniil.bulletinBoard.entity.base.order.OrderItem;
import ru.daniil.bulletinBoard.entity.base.product.Product;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.entity.request.CreateOrderItemRequest;
import ru.daniil.bulletinBoard.entity.request.DeleteOrderItemRequest;
import ru.daniil.bulletinBoard.service.orderItem.OrderItemService;
import ru.daniil.bulletinBoard.service.product.ProductService;
import ru.daniil.bulletinBoard.service.user.UserService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderProcessorServiceImpl implements OrderProcessorService {
    private final UserService userService;
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final ProductService productService;

    public OrderProcessorServiceImpl(UserService userService, OrderService orderService,
                                     OrderItemService orderItemService,
                                     ProductService productService) {
        this.userService = userService;
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.productService = productService;
    }

    @Transactional
    @Override
    public OrderItem addOrderItem(CreateOrderItemRequest request) {
        User user = userService.getAuthUser();
        Order order = orderService.getLastOrCreateOrderByUser(user);
        Product product = productService.getBySku(request.getSku());

        if (product.getStockQuantity() >= request.getQuantity()){
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
    public void removeOrderItem(DeleteOrderItemRequest request) {
        User user = userService.getAuthUser();
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
