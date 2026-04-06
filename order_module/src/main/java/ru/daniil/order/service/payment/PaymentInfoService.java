package ru.daniil.order.service.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.daniil.core.entity.base.payment.PaymentInfo;
import ru.daniil.core.response.PaymentInfoResponse;
import ru.daniil.core.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentInfoService {
    PaymentInfoResponse createPayment(PaymentInfo paymentInfo);

    Optional<PaymentInfo> getPaymentById(Long id);

    Page<PaymentInfo> getAllPaymentsOrderedByCreatedAtDesc(Pageable pageable);

    PaymentInfo getPaymentByOrderNumber(String orderNumber);

    List<PaymentInfo> getPaymentsByStatus(String status);

    PaymentInfo updatePayment(Long id, PaymentStatus status);

    PaymentInfoResponse updatePaymentStatus(PaymentInfo item, String status);

    void deletePayment(Long id);

    List<PaymentInfo> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

}
