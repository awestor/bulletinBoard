package ru.daniil.bulletinBoard.repository.order;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.daniil.bulletinBoard.entity.base.payment.PaymentMethod;

import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends CrudRepository<PaymentMethod, Long> {
    Optional<PaymentMethod> findByType(String type);
    boolean existsByType(String type);
}
