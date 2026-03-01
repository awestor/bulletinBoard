package ru.daniil.bulletinBoard.repository.product;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.daniil.bulletinBoard.entity.base.product.Category;
import ru.daniil.bulletinBoard.entity.base.product.Product;

import java.util.List;

@Repository
public interface ProductRepository extends CrudRepository<Product, Long> {
    @Modifying
    @Query("UPDATE Product p SET p.category = :newCategory WHERE p IN :products")
    void updateCategoryForProducts(@Param("products") List<Product> products,
                                  @Param("newCategory") Category newCategory);

    List<Product> findByCategoryName(String categoryName);

    Product findBySku(String sku);
}
