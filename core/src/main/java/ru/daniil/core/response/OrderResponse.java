package ru.daniil.core.response;

import lombok.Builder;
import lombok.Data;
import ru.daniil.core.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderResponse {
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
}