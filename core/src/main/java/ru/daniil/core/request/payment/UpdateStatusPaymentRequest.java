package ru.daniil.core.request.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusPaymentRequest {

    @NotBlank(message = "Номер заказа не может быть пустым")
    @Size(min = 3, max = 50, message = "Номер заказа должен быть от 3 до 50 символов")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Номер заказа может содержать только буквы, цифры и дефисы")
    private String orderNumber;
}