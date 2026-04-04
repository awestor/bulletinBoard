package ru.daniil.core.entity.base.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Entity
@Table(name = "tbl_product_attributes")
@Data
@Builder
@AllArgsConstructor
public class ProductAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String key;

    @Column(nullable = false)
    private String value;

    public ProductAttribute() {}

    public ProductAttribute(Product product, String key, String value) {
        this.product = product;
        this.key = key;
        this.value = value;
    }

}
