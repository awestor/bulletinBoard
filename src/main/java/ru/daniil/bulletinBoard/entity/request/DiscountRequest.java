package ru.daniil.bulletinBoard.entity.request;

import jakarta.validation.constraints.*;
import ru.daniil.bulletinBoard.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DiscountRequest {
    @NotBlank(message = "Discount code is required")
    @Pattern(regexp = "^[A-Z0-9_-]{3,50}$",
            message = "Discount code must contain only uppercase letters, numbers, underscore and hyphen (3-50 characters)")
    private String code;

    @NotBlank(message = "Discount name is required")
    @Size(min = 3, max = 200, message = "Discount name must be between 3 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Discount type is required")
    private DiscountType type;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Discount value cannot exceed 999999.99")
    private BigDecimal value;

    @Positive(message = "Category ID must be positive")
    private Long applicableCategoryId;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    @NotNull(message = "Usage limit is required")
    @Min(value = 1, message = "Usage limit must be at least 1")
    @Max(value = 999999, message = "Usage limit cannot exceed 999999")
    private Integer usageLimit;

    @AssertTrue(message = "End date must be after start date")
    private boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
}
