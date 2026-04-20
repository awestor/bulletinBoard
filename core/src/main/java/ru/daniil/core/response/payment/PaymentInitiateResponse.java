package ru.daniil.core.response.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiateResponse {
    private String orderNumber;
    private BigDecimal totalPrice;
    private String status;
    private String message;
    private LocalDateTime createdAt;
}