package ru.daniil.order.service.orderItem;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.entity.base.discount.OrderDiscount;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.response.orderItem.OrderItemsCalculationResult;
import ru.daniil.order.repository.order.OrderItemRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        List<OrderItem> orderItems = orderItemRepository.findByOrderNumberWithProduct(order.getOrderNumber());

        return orderItems.stream()
                .allMatch(item -> item.getProduct().getStockQuantity() >= item.getQuantity());
    }

    @Override
    public void delete(Long orderItemId) {
        OrderItem item = getById(orderItemId);
        orderItemRepository.delete(item);
    }

    @Override
    public Integer countReservation(String sku){
        return orderItemRepository.getTotalReservedQuantityByProductSku(sku, LocalDateTime.now());
    }

    @Override
    public OrderItemsCalculationResult calculateOrderItemsPrices(Order order,
                                                                 Map<Long, List<OrderDiscount>> discountsByCategory) {
        List<OrderItem> items = getByOrderNumber(order.getOrderNumber());
        List<OrderItem> updatedItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItem item : items) {
            Product product = item.getProduct();
            if (product == null) continue;

            BigDecimal priceAtTime = product.getPriceAtTime();
            BigDecimal priceWithoutDisc = priceAtTime.multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal maxDiscount = calculateMaxDiscount(priceWithoutDisc, product, discountsByCategory);

            BigDecimal discountedPrice = priceWithoutDisc.subtract(maxDiscount);
            totalPrice = totalPrice.add(discountedPrice);

            // Создаем копию item с обновленной ценой (не модифицируем оригинал)
            item.setPriceAtTime(discountedPrice);
            updatedItems.add(item);
        }

        return OrderItemsCalculationResult.builder()
                .updatedItems(updatedItems)
                .totalPrice(totalPrice)
                .build();
    }

    @Override
    @Transactional
    public void applyCalculatedPrices(OrderItemsCalculationResult calculationResult) {
        for (OrderItem updatedItem : calculationResult.getUpdatedItems()) {
            updatePriceAtTime(updatedItem);
        }
    }

    private BigDecimal calculateMaxDiscount(BigDecimal priceWithoutDisc,
                                            Product product,
                                            Map<Long, List<OrderDiscount>> discountsByCategory) {
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

        return maxDiscount;
    }
}