package ru.daniil.bulletinBoard.entity.base.discount;

import jakarta.persistence.*;
import ru.daniil.bulletinBoard.entity.base.order.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_order_discounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "discount_id"}))
public class OrderDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "reserved_until", nullable = false)
    private LocalDateTime reservedUntil;

    public OrderDiscount() {
        createdAt = LocalDateTime.now();
    }

    public OrderDiscount(Order order, Discount discount) {
        this();
        this.order = order;
        this.discount = discount;
        this.discountAmount = calculateDiscountAmount(order, discount);
        this.reservedUntil = LocalDateTime.now().plusMinutes(20);
    }

    private BigDecimal calculateDiscountAmount(Order order, Discount discount) {
        if (discount.getPercentage() != null) {
            return order.getTotalPrice()
                    .multiply(discount.getPercentage())
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        }
        return discount.getFixedAmount() != null ? discount.getFixedAmount() : BigDecimal.ZERO;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public Discount getDiscount() {
        return discount;
    }
}