package ru.daniil.core.response.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.daniil.core.entity.base.user.User;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerAmountDto {

    private User seller;

    private BigDecimal amount;
}
