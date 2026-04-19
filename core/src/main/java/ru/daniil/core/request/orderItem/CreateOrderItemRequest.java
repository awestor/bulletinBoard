package ru.daniil.core.request.orderItem;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderItemRequest {
    @NotBlank(message = "SKU товара не может быть пустым")
    @Size(min = 3, max = 20, message = "SKU должно содержать от 3 до 20 символов")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "SKU может содержать только заглавные буквы, цифры и дефис")
    private String sku;

    @NotNull(message = "Количество товара обязательно")
    @Min(value = 1, message = "Количество товара должно быть не меньше 1")
    @Max(value = 999, message = "Количество товара не может превышать 999")
    private Integer quantity;
}
