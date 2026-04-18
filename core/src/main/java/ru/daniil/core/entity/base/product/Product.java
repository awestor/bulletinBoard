package ru.daniil.core.entity.base.product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "tbl_products")
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"attributes"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private BigDecimal priceAtTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id")
    private Discount discount;

    @Column (unique = true)
    private String sku;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)  // ← НОВОЕ ПОЛЕ
    private User seller;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductAttribute> attributes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Product() {
        images = new ArrayList<>();
        attributes = new HashSet<>();
    }

    public Product(Category category,
                   User seller,
                   String name, BigDecimal price) {
        this();
        this.category = category;
        this.seller = seller;
        this.name = name;
        this.price = price;
        this.priceAtTime = price;
        this.status = ProductStatus.ACTIVE;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        updatePriceAtTime();
    }

    public void applyDiscount(Discount discount) {
        this.discount = discount;
        updatePriceAtTime();
    }

    public void removeDiscount() {
        this.discount = null;
        updatePriceAtTime();
    }

    private void updatePriceAtTime() {
        if (discount == null) {
            this.priceAtTime = this.price;
        } else {
            this.priceAtTime = price
                    .multiply(BigDecimal.valueOf(100).subtract(
                            discount.getPercentage() != null ? discount.getPercentage() : BigDecimal.ZERO))
                    .subtract(discount.getFixedAmount() != null ? discount.getFixedAmount() : BigDecimal.ZERO);
        }
    }

    public void addAttribute(String key, String value) {
        ProductAttribute attribute = new ProductAttribute(this, key, value);
        this.attributes.add(attribute);
    }

    public String getAttribute(String key) {
        return attributes.stream()
                .filter(attr -> attr.getKey().equals(key))
                .map(ProductAttribute::getValue)
                .findFirst()
                .orElse(null);
    }

    public Map<String, String> getAttributesMap() {
        return attributes.stream()
                .collect(Collectors.toMap(
                        ProductAttribute::getKey,
                        ProductAttribute::getValue
                ));
    }

    public void setAttributes(Set<ProductAttribute> attributes) {
        this.attributes.clear();
        this.attributes = attributes;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
