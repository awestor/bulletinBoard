package ru.daniil.bulletinBoard.entity.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentInfoResponse {
    private String orderNumber;

    private String paymentMethodType;

    private BigDecimal paidCost;

    private String status;

    public PaymentInfoResponse() {
    }

    public PaymentInfoResponse(String orderNumber, String paymentMethodType, BigDecimal paidCost, String status) {
        this.orderNumber = orderNumber;
        this.paymentMethodType = paymentMethodType;
        this.paidCost = paidCost;
        this.status = status;
    }
}