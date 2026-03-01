package ru.daniil.bulletinBoard.repository.product;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.daniil.bulletinBoard.entity.base.product.ProductAttribute;

@Repository
public interface ProductAttributeRepository extends CrudRepository<ProductAttribute, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductAttribute pa WHERE pa.key = :key")
    void deleteAttributeForProducts(@Param("key") String key);
}
