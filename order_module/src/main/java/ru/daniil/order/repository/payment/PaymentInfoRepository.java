package ru.daniil.order.repository.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.payment.PaymentInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentInfoRepository extends CrudRepository<PaymentInfo, Long> {

    Optional<PaymentInfo> findByOrderNumber(String orderNumber);

    List<PaymentInfo> findByStatus(String status);

    Page<PaymentInfo> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<PaymentInfo> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
