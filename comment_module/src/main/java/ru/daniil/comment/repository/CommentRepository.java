package ru.daniil.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.product.Comment;
import ru.daniil.core.entity.base.user.User;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c WHERE c.product.sku = :sku")
    Page<Comment> findByProductSku(@Param("sku") String sku, Pageable pageable);

    Optional<Comment> findByUserAndProductSku(User user, String sku);

    @Query("SELECT AVG(c.rating) FROM Comment c WHERE c.product.id = :productId")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
