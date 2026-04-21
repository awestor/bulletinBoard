package ru.daniil.core.request.discount;

import jakarta.validation.constraints.*;
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
public class CreateDiscountRequest {
    @NotBlank(message = "Код акции обязателен")
    @Pattern(regexp = "^[A-Z0-9_-]{3,50}$",
            message = "Код акции должен состоять только из Заглавных букв латинского алфавита, '-', '_' и цифр (3-50 символов)")
    private String code;

    @NotBlank(message = "Название акции обязательно")
    @Size(min = 3, max = 200, message = "Название акции должно состоять от 3 до 200 символов")
    private String name;

    @Size(max = 1000, message = "Описание не может быть более 1000 символов")
    private String description;

    @NotNull(message = "Тип акции обязателен")
    private String type;

    @NotNull(message = "Величина скидки по акции обязательна")
    @DecimalMin(value = "0.00", message = "Значение не может быть меньше 0")
    @DecimalMax(value = "999999.99", message = "Значение не может быть 999999.99 или больше")
    private BigDecimal valueAmount;

    @NotNull(message = "Величина скидки по акции обязательна")
    @DecimalMin(value = "0.00", message = "Значение процентов не может быть меньше 0")
    @DecimalMax(value = "99.99", message = "Значение процентов не может быть 99.99 или больше")
    private BigDecimal valuePercentage;

    @Positive(message = "ID категории должно быть положительным")
    private Long applicableCategoryId;

    @NotNull(message = "Дата начала акции обязательна")
    @FutureOrPresent(message = "Дата начала акции не может быть оформлена задним числом")
    private LocalDateTime startDate;

    @NotNull(message = "Дата конца акции обязательна")
    @Future(message = "Дата конца акции не может быть оформлена на момент начала акции")
    private LocalDateTime endDate;

    @NotNull(message = "Максимальное количество использований должно быть объявлено")
    @Min(value = 1, message = "Максимальное количество использований должно быть 1 или больше")
    @Max(value = 9999999, message = "Максимальное количество использований не должно превышать 9999999 раз")
    private Integer usageLimit;

    @AssertTrue(message = "Дата начала акции не должна быть позже даты окончания акции")
    private boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
}
