package ru.daniil.core.request.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {

    @NotNull(message = "Сумма пополнения не может быть пустой")
    @DecimalMin(value = "100", message = "Сумма пополнения должна быть не менее 100")
    private BigDecimal amount;

    @NotBlank(message = "Внешний ID депозита не может быть пустым")
    @Size(min = 3, max = 100, message = "Внешний ID депозита должен быть от 3 до 100 символов")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "ID может содержать только буквы, цифры, дефисы и подчеркивания")
    @JsonProperty("external_deposit_id")
    private String externalDepositId;

    @Size(max = 500, message = "Описание не может превышать 500 символов")
    private String description;
}