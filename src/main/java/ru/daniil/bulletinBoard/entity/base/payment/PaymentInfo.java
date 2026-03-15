package ru.daniil.bulletinBoard.entity.base.payment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import ru.daniil.bulletinBoard.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_payment_info")
@Data
@Builder
@AllArgsConstructor
public class PaymentInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod method;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column
    private LocalDateTime createdAt;

    public PaymentInfo() {
        this.status = PaymentStatus.PROCESSING;
    }

    public PaymentInfo(String orderNumber, BigDecimal totalPrice) {
        this();
        this.orderNumber = orderNumber;
        this.totalPrice = totalPrice;
        this.method = null;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
