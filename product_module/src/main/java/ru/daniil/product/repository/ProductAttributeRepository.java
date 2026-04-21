package ru.daniil.product.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.product.ProductAttribute;

@Repository
public interface ProductAttributeRepository extends CrudRepository<ProductAttribute, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductAttribute pa WHERE pa.key = :key")
    void deleteAttributeForProducts(@Param("key") String key);

    Page<ProductAttribute> findByProduct(Product product, Pageable pageable);
}
