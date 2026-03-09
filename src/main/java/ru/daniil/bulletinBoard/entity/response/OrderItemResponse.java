package ru.daniil.bulletinBoard.entity.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderItemResponse {
    private String productName;
    private String productSku;
    private Integer quantity;
    private BigDecimal priceAtTime;
    private LocalDateTime reservedUntil;
    private boolean reservationExpired;
}