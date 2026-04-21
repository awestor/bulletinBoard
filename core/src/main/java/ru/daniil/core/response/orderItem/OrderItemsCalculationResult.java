package ru.daniil.core.response.orderItem;

import lombok.Builder;
import lombok.Data;
import ru.daniil.core.entity.base.order.OrderItem;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderItemsCalculationResult {
    private List<OrderItem> updatedItems;
    private BigDecimal totalPrice;
}
