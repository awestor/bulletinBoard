package ru.daniil.image.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.product.ProductImage;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends CrudRepository<ProductImage, Long> {
    /**
     * Находит все изображения для конкретного продукта
     * @param productId идентификатор продукта
     * @return список всех найденных изображений List<ProductImage>
     */
    List<ProductImage> findByProductId(Long productId);

    /**
     * Находит запись о файле в БД
     * @param name название сохранённого упоминания о файле в БД
     * @return найденную запись
     */
    Optional<ProductImage> findByName(String name);


    /**
     * Удаляет все изображения для конкретного продукта
     * @param productId идентификатор продукта
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductImage pi WHERE pi.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    /**
     * Удаляет !!!все!!! изображения из БД
     */
    @Modifying
    @Query("DELETE FROM ProductImage")
    void deleteAllInBatch();
}
