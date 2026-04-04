package ru.daniil.core.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteOrderItemRequest {
    @NotBlank(message = "SKU товара не может быть пустым")
    @Size(min = 3, max = 20, message = "SKU должно содержать от 3 до 20 символов")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "SKU может содержать только заглавные буквы, цифры и дефис")
    private String sku;

    public DeleteOrderItemRequest() {
    }

    public DeleteOrderItemRequest(String sku) {
        this.sku = sku;
    }
}
