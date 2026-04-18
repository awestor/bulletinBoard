package ru.daniil.product.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public interface ProductRepository extends CrudRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Modifying
    @Query("UPDATE Product p SET p.category = :newCategory WHERE p IN :products")
    void updateCategoryForProducts(@Param("products") List<Product> products,
                                  @Param("newCategory") Category newCategory);

    List<Product> findByCategoryName(String categoryName);

    Product findBySku(String sku);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    /**
     * Создание спецификации для большой фильтрации
     * @param inStock есть в наличии или нет
     * @param minPrice мин цена поиска
     * @param maxPrice макс цена поиска
     * @param namePart часть названия продукта
     * @param skuPart часть sku продукта
     * @param categoryName название категории
     * @param sellerLogin login продавца
     * @return спецификация поиска
     */
    static Specification<Product> withFilters(Boolean inStock,
                                              BigDecimal minPrice,
                                              BigDecimal maxPrice,
                                              String namePart,
                                              String skuPart,
                                              String categoryName,
                                              String sellerLogin) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (inStock != null) {
                if (inStock) {
                    predicates.add(cb.gt(root.get("stockQuantity"), 0));
                } else {
                    predicates.add(cb.le(root.get("stockQuantity"), 0));
                }
            }

            if (minPrice != null) {
                predicates.add(cb.ge(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.le(root.get("price"), maxPrice));
            }

            if (namePart != null && !namePart.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + namePart.toLowerCase() + "%"));
            }

            if (skuPart != null && !skuPart.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("sku")), "%" + skuPart.toLowerCase() + "%"));
            }

            if (categoryName != null && !categoryName.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category").get("name")), categoryName.toLowerCase()));
            }

            if (sellerLogin != null && !sellerLogin.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("seller").get("login")), sellerLogin.toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Modifying
    @Query("DELETE FROM Product")
    void deleteAllInBatch();

    Page<Product> findAll(Pageable pageable);
}
