package ru.daniil.bulletinBoard.entity.base.order;

import jakarta.persistence.*;
import ru.daniil.bulletinBoard.entity.base.discount.Discount;
import ru.daniil.bulletinBoard.entity.base.discount.OrderDiscount;
import ru.daniil.bulletinBoard.entity.base.payment.PaymentInfo;
import ru.daniil.bulletinBoard.entity.base.product.Product;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.enums.OrderStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "tbl_orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String status;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDiscount> appliedDiscounts;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Order() {
        this.orderNumber = generateOrderNumber();
        this.status = OrderStatus.DRAFT.toString();
        items = new ArrayList<>();
        appliedDiscounts = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Order(User user) {
        this();
        this.user = user;
    }

    public String getStatus() {
        return status;
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderItem findItemByProduct(Product product) {
        return items.stream()
                .filter(item -> item.getProduct().equals(product))
                .findFirst()
                .orElse(null);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<OrderDiscount> getAppliedDiscounts() {
        return appliedDiscounts;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

