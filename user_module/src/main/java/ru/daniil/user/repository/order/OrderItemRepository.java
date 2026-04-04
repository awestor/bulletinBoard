package ru.daniil.user.repository.order;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.order.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends CrudRepository<OrderItem, Long> {
    List<OrderItem> findByOrder_OrderNumber(String orderNumber);

    @Modifying
    @Query("UPDATE OrderItem oi SET oi.quantity = :quantity, oi.reservedUntil = :reservedUntil WHERE oi.id = :id")
    int updateQuantity(@Param("id") Long id,
                       @Param("quantity") Integer quantity,
                       @Param("reservedUntil") LocalDateTime reservedUntil);

    @Modifying
    @Query("UPDATE OrderItem oi SET oi.priceAtTime = :priceAtTime WHERE oi.id = :id")
    int updatePrice(@Param("id") Long id, @Param("priceAtTime") BigDecimal priceAtTime);
}
