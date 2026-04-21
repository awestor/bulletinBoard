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
     * Находит все изображения для конкретного продукта по id
     * @param productId идентификатор продукта
     * @return список всех найденных изображений List<ProductImage>
     */
    List<ProductImage> findByProductId(Long productId);

    /**
     * Находит все изображения для конкретного продукта по артикулу
     * @param sku артикул продукта
     * @return список всех найденных изображений List<ProductImage>
     */
    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.sku = :sku")
    List<ProductImage> findByProductSku(@Param("sku") String sku);

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

    @Modifying
    @Transactional
    @Query("UPDATE ProductImage pi1 SET pi1.isMain = false WHERE pi1.product.id = " +
            "(SELECT pi2.product.id FROM ProductImage pi2 WHERE pi2.fileName = :fileName)")
    void unsetOtherMainImages(@Param("fileName") String fileName);

    @Modifying
    @Transactional
    @Query("UPDATE ProductImage pi SET pi.isMain = true WHERE pi.fileName = :fileName")
    void setMainByFileName(@Param("fileName") String fileName);

    /**
     * Удаляет !!!все!!! изображения из БД
     */
    @Modifying
    @Query("DELETE FROM ProductImage")
    void deleteAllInBatch();
}
