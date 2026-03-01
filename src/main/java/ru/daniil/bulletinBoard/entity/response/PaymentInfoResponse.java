package ru.daniil.bulletinBoard.entity.response;

import java.math.BigDecimal;

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

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getPaymentMethodType() {
        return paymentMethodType;
    }

    public void setPaymentMethodType(String paymentMethodType) {
        this.paymentMethodType = paymentMethodType;
    }

    public BigDecimal getPaidCost() {
        return paidCost;
    }

    public void setPaidCost(BigDecimal paidCost) {
        this.paidCost = paidCost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}