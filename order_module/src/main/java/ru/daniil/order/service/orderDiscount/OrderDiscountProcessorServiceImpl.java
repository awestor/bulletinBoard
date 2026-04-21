package ru.daniil.order.service.orderDiscount;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.entity.base.discount.OrderDiscount;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.enums.OrderStatus;
import ru.daniil.core.exceptions.DiscountNotApplicableException;
import ru.daniil.core.sharedInterfaces.DiscountProvider;
import ru.daniil.order.service.order.OrderPriceRecalculationService;
import ru.daniil.order.service.order.OrderService;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderDiscountProcessorServiceImpl implements OrderDiscountProcessorService {
    private final OrderService orderService;
    private final DiscountProvider discountProvider;
    private final OrderDiscountService orderDiscountService;
    private final OrderPriceRecalculationService orderPriceRecalculationService;

    public OrderDiscountProcessorServiceImpl(OrderService orderService,
                                             DiscountProvider discountProvider,
                                             OrderDiscountService orderDiscountService,
                                             OrderPriceRecalculationService orderPriceRecalculationService) {
        this.orderService = orderService;
        this.discountProvider = discountProvider;
        this.orderDiscountService = orderDiscountService;
        this.orderPriceRecalculationService = orderPriceRecalculationService;
    }

    @Transactional
    @Override
    public OrderDiscount applyDiscountToOrder(String orderNumber, String discountCode, User user) {
        Order order = orderService.getLastOrCreateOrderByUser(user);

        if (!order.getOrderNumber().equals(orderNumber) || order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalArgumentException("Заказ с указанным номером не найден или недоступен");
        }

        Discount discount = discountProvider.getActiveDiscountByCode(discountCode);

        if (orderDiscountService.getOrderDiscountByIds(order.getId(), discount.getId()).isPresent()) {
            throw new IllegalArgumentException("Эта скидка уже применена к заказу");
        }

        discountProvider.incrementUsageCount(discount.getId());

        try {
            BigDecimal newTotal = orderPriceRecalculationService.previewTotalWithNewDiscount(order, discount);

            if (order.getTotalPrice().compareTo(newTotal) == 0) {
                throw new DiscountNotApplicableException(
                        "Эта скидка не влияет на итоговую сумму заказа. " +
                                "Возможно, уже применена более выгодная скидка на эти товары.");
            }

            OrderDiscount orderDiscount = new OrderDiscount(order, discount);
            OrderDiscount saved = orderDiscountService.save(orderDiscount);

            orderPriceRecalculationService.recalculateTotals(order);

            return saved;

        } catch (Exception e) {
            discountProvider.decrementUsageCount(discount.getId());
            throw e;
        }
    }

    @Transactional
    @Override
    public void removeDiscountFromOrder(String orderNumber, String discountCode, User user) {
        Order order = orderService.getLastOrCreateOrderByUser(user);

        if (!order.getOrderNumber().equals(orderNumber) || order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalArgumentException("Заказ с указанным номером не найден или недоступен");
        }

        Discount discount = discountProvider.getActiveDiscountByCode(discountCode);

        orderDiscountService.deleteByIds(order.getId(), discount.getId());
    }

    @Override
    public List<OrderDiscount> getAppliedDiscounts(String orderNumber, User user) {
        Order order = orderService.getByOrderNumber(orderNumber);
        if (!order.getUser().getEmail().equals(user.getEmail())){
            throw new IllegalArgumentException(
                    "Номер заказа не соответствует ни одному из заказов пользователя");
        }
        return orderDiscountService.getOrderDiscountByOrderId(order.getId());
    }
}
