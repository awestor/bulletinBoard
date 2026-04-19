package ru.daniil.core.response.wallet;

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
public class WalletTransactionResponse {

    private Long id;

    private BigDecimal amount;

    private String transactionType;

    private String status;

    private String description;

    private String externalId;

    private LocalDateTime createdAt;
}
