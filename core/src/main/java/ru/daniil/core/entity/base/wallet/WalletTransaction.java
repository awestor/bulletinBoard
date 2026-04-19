package ru.daniil.core.entity.base.wallet;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.enums.wallet.TransactionStatus;
import ru.daniil.core.enums.wallet.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_wallet_transaction",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_transaction_unique_active",
                        columnNames = {"order_id", "external_deposit", "transaction_type"}
                )
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.DRAFT;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private Order order;

    @Column(name = "external_deposit")
    private String externalDeposit;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}