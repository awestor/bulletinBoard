package ru.daniil.order.service.order;

import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.entity.base.discount.OrderDiscount;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.response.orderItem.OrderItemsCalculationResult;
import ru.daniil.order.service.orderDiscount.OrderDiscountService;
import ru.daniil.order.service.orderItem.OrderItemService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderPriceRecalculationServiceImpl implements OrderPriceRecalculationService {

    private final OrderService orderService;
    private final OrderDiscountService orderDiscountService;
    private final OrderItemService orderItemService;

    public OrderPriceRecalculationServiceImpl(OrderService orderService, OrderDiscountService orderDiscountService, OrderItemService orderItemService) {
        this.orderService = orderService;
        this.orderDiscountService = orderDiscountService;
        this.orderItemService = orderItemService;
    }

    public void recalculateTotals(Order order) {
        Map<Long, List<OrderDiscount>> discountsByCategory = getCurrentDiscountsGrouped(order);

        OrderItemsCalculationResult calculationResult =
                orderItemService.calculateOrderItemsPrices(order, discountsByCategory);

        orderItemService.applyCalculatedPrices(calculationResult);
        order.setTotalPrice(calculationResult.getTotalPrice());
        orderService.updateTotalPrice(order);
    }

    public BigDecimal previewTotalWithNewDiscount(Order order, Discount newDiscount) {
        Map<Long, List<OrderDiscount>> discountsByCategory =
                getCurrentDiscountsWithNewDiscount(order, newDiscount);

        OrderItemsCalculationResult calculationResult =
                orderItemService.calculateOrderItemsPrices(order, discountsByCategory);

        return calculationResult.getTotalPrice();
    }

    private Map<Long, List<OrderDiscount>> getCurrentDiscountsGrouped(Order order) {
        return orderDiscountService.getOrderDiscountByOrderId(order.getId()).stream()
                .collect(Collectors.groupingBy(
                        d -> d.getDiscount().getApplicableCategoryId()));
    }

    private Map<Long, List<OrderDiscount>> getCurrentDiscountsWithNewDiscount(Order order, Discount newDiscount) {
        List<OrderDiscount> existingDiscounts = orderDiscountService.getOrderDiscountByOrderId(order.getId());
        List<OrderDiscount> tempDiscounts = new ArrayList<>(existingDiscounts);

        OrderDiscount tempOrderDiscount = new OrderDiscount();
        tempOrderDiscount.setDiscount(newDiscount);
        tempDiscounts.add(tempOrderDiscount);

        return tempDiscounts.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getDiscount().getApplicableCategoryId()));
    }
}
