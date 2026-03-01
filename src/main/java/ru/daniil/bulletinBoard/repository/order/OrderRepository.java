package ru.daniil.bulletinBoard.repository.order;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.daniil.bulletinBoard.entity.base.order.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends CrudRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<Order> findLastByUserId(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Order o SET o.totalPrice = :totalPrice WHERE o.id = :id")
    int updateTotalPrice(@Param("id") Long id, @Param("totalPrice") BigDecimal totalPrice);

    List<Order> findByUserId(Long userId);
}
