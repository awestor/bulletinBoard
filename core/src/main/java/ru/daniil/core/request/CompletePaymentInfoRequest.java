package ru.daniil.core.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompletePaymentInfoRequest {
    @NotBlank(message = "Номер заказа обязателен")
    @Size(max = 50, message = "Номер заказа не должен превышать 28 characters")
    private String orderNumber;

    @NotNull(message = "Название типа не может превышать 50 символов")
    private String paymentMethodType;

    @NotBlank(message = "Статус должен указан")
    private String status;

    public CompletePaymentInfoRequest() {
    }

    public CompletePaymentInfoRequest(String orderNumber, String paymentMethodType,
                                      String status) {
        this.orderNumber = orderNumber;
        this.paymentMethodType = paymentMethodType;
        this.status = status;
    }
}
