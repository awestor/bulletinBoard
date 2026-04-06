package ru.daniil.order.repository.discount;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.discount.OrderDiscount;

@Repository
public interface OrderDiscountRepository extends CrudRepository<OrderDiscount, Long> {
}
