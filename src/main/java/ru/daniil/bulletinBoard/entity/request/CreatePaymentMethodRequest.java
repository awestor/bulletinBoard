package ru.daniil.bulletinBoard.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreatePaymentMethodRequest {
    @NotBlank(message = "Тип способа оплаты обязателен")
    @Size(max = 50, message = "Название типа не может превышать более 50 символов")
    private String type;

    @Size(max = 500, message = "Описание способа оплаты не должно превышать 500 символов")
    private String description;

    public CreatePaymentMethodRequest() {
    }

    public CreatePaymentMethodRequest(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
