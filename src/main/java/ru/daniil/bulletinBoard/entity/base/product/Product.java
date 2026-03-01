package ru.daniil.bulletinBoard.entity.base.product;
import jakarta.persistence.*;
import ru.daniil.bulletinBoard.entity.base.discount.Discount;
import ru.daniil.bulletinBoard.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "tbl_products")
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

    @Column
    private String status;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductAttribute> attributes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Product() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        images = new ArrayList<>();
        attributes = new HashSet<>();
    }

    public Product(Category category, String name, BigDecimal price) {
        this();
        this.category = category;
        this.name = name;
        this.price = price;
        this.priceAtTime = price;
        this.status = ProductStatus.ACTIVE.toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return category.getId();
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        updatePriceAtTime();
    }

    public BigDecimal getPriceAtTime() {
        return priceAtTime;
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

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public void addAttribute(String key, String value) {
        ProductAttribute attribute = new ProductAttribute(this, key, value);
        this.attributes.add(attribute);
    }

    public void addAttribute(String key, Object value) {
        addAttribute(key, String.valueOf(value));
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

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Set<ProductAttribute> getAttributes() {
        return attributes;
    }
}
