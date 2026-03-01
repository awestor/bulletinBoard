package ru.daniil.bulletinBoard.entity.base.order;

import jakarta.persistence.*;
import ru.daniil.bulletinBoard.entity.base.product.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_order_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "order_id"}))
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

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(reservedUntil);
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Количество товаров в позиции заказа должно быть больше нуля");
        }
        this.quantity = quantity;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getPriceAtTime() {
        return priceAtTime;
    }

    public void setPriceAtTime(BigDecimal priceAtTime) {
        this.priceAtTime = priceAtTime;
    }

    @PreUpdate
    protected void onUpdate() {
        reservedUntil = LocalDateTime.now().plusMinutes(20);
    }
}