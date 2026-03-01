package ru.daniil.bulletinBoard.entity.base.payment;

import jakarta.persistence.*;
import ru.daniil.bulletinBoard.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_payment_info")
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

    @Column
    private String status;

    @Column
    private LocalDateTime createdAt;

    public PaymentInfo() {
        this.status = PaymentStatus.PROCESSING.toString();
        this.createdAt = LocalDateTime.now();
    }

    public PaymentInfo(String orderNumber, BigDecimal totalPrice) {
        this();
        this.orderNumber = orderNumber;
        this.totalPrice = totalPrice;
        this.method = null;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
