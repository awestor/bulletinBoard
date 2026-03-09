package ru.daniil.bulletinBoard.entity.base.discount;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import ru.daniil.bulletinBoard.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_discounts")
@Data
@Builder
@AllArgsConstructor
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column
    private String name;

    @Column
    private String description;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(precision = 10, scale = 2)
    private BigDecimal fixedAmount;

    @Column(name = "applicable_category_id")
    private Long applicableCategoryId;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count")
    private Integer usageCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private DiscountType type;

    public Discount() {
        applicableCategoryId = -1L;
        usageCount = 0;
        createdAt = LocalDateTime.now();
    }

    public Discount(String code, String name, String description,
                    LocalDateTime startDate, LocalDateTime endDate,
                    Integer usageLimit, DiscountType type) {
        this();
        this.code = code;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usageLimit = usageLimit;
        this.type = type;
    }
}
