package ru.daniil.bulletinBoard.repository.discount;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.daniil.bulletinBoard.entity.base.discount.OrderDiscount;

@Repository
public interface OrderDiscountRepository extends CrudRepository<OrderDiscount, Long> {
}
