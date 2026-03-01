package ru.daniil.bulletinBoard.entity.request;

import jakarta.validation.constraints.*;

public class CreatePaymentInfoRequest {
    @NotBlank(message = "Номер заказа обязателен")
    @Size(max = 50, message = "Номер заказа не должен превышать 28 characters")
    private String orderNumber;

    @NotNull(message = "Название типа не может превышать 50 символов")
    private String paymentMethodType;

    public CreatePaymentInfoRequest() {
    }

    public CreatePaymentInfoRequest(String orderNumber, String paymentMethodType) {
        this.orderNumber = orderNumber;
        this.paymentMethodType = paymentMethodType;
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
}
