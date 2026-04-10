package ru.daniil.core.entity.base.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Entity
@Table(name = "tbl_product_image")
@Data
@Builder
@AllArgsConstructor
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String name;

    @Column
    private Boolean isMain;

    public ProductImage() {}

    public ProductImage(Product product, String name, boolean isMain) {
        this.product = product;
        this.name = name;
        this.isMain = isMain;
    }
}
