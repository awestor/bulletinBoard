package ru.daniil.bulletinBoard.repository.product;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.daniil.bulletinBoard.entity.base.product.Category;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends CrudRepository<Category, Long> {
    List<Category> findByParentIsNull();

    List<Category> findByParentId(Long parentId);

    List<Category> findByType(String type);

    Optional<Category> findByName(String type);

    @Modifying
    @Query("UPDATE Category c SET c.parent = :newParent WHERE c IN :children")
    void updateParentForCategories(@Param("children") List<Category> children,
                                  @Param("newParent") Category newParent);
}
