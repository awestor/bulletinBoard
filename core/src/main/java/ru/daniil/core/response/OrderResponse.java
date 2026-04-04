package ru.daniil.core.response;

import lombok.Builder;
import lombok.Data;
import ru.daniil.core.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private List<OrderItemResponse> items;
    private List<OrderDiscountResponse> appliedDiscounts;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}