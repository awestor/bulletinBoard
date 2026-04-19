package ru.daniil.order.repository.order;

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

    @Query("SELECT oi FROM OrderItem oi " +
            "JOIN FETCH oi.product " +
            "WHERE oi.order.orderNumber = :orderNumber")
    List<OrderItem> findByOrderNumberWithProduct(@Param("orderNumber") String orderNumber);

    @Modifying
    @Query("UPDATE OrderItem oi SET oi.quantity = :quantity, oi.reservedUntil = :reservedUntil WHERE oi.id = :id")
    int updateQuantity(@Param("id") Long id,
                       @Param("quantity") Integer quantity,
                       @Param("reservedUntil") LocalDateTime reservedUntil);

    @Modifying
    @Query("UPDATE OrderItem oi SET oi.priceAtTime = :priceAtTime WHERE oi.id = :id")
    int updatePrice(@Param("id") Long id, @Param("priceAtTime") BigDecimal priceAtTime);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
            "WHERE oi.product.sku = :sku AND oi.reservedUntil > :now")
    Integer getTotalReservedQuantityByProductSku(@Param("sku") String sku,
                                                 @Param("now") LocalDateTime now);
}
