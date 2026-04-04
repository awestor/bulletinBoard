package ru.daniil.core.entity.base.order;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import ru.daniil.core.entity.base.product.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_order_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "order_id"}))
@Data
@Builder
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_at_time", nullable = false)
    private BigDecimal priceAtTime;

    @Column(name = "reserved_until", nullable = false)
    private LocalDateTime reservedUntil;

    public OrderItem() {
    }

    public OrderItem(Order order, Product product, Integer quantity) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.reservedUntil = generateReservationTime();
    }

    public LocalDateTime generateReservationTime(){
        return LocalDateTime.now().plusMinutes(20);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(reservedUntil);
    }

    public void setQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Количество товаров в позиции заказа должно быть больше нуля");
        }
        this.quantity = quantity;
    }

    @PreUpdate
    protected void onUpdate() {
        reservedUntil = LocalDateTime.now().plusMinutes(20);
    }
}