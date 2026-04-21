package ru.daniil.order.repository.discount;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.discount.OrderDiscount;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderDiscountRepository extends CrudRepository<OrderDiscount, Long> {

    List<OrderDiscount> findByOrderId(Long orderId);

    Optional<OrderDiscount> findByOrderIdAndDiscountId(Long orderId, Long discountId);

    @Modifying
    @Query("DELETE FROM OrderDiscount od WHERE od.order.id = :orderId AND od.discount.id = :discountId")
    void deleteByOrderIdAndDiscountId(@Param("orderId") Long orderId, @Param("discountId") Long discountId);
}
