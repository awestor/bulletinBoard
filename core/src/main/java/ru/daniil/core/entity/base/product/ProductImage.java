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
    private String path;

    @Column
    private Boolean isMain;

    public ProductImage() {}

    public ProductImage(Product product, String path, boolean isMain) {
        this.product = product;
        this.path = path;
        this.isMain = isMain;
    }
}
