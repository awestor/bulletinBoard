package ru.daniil.user.service.payment;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.payment.PaymentInfo;
import ru.daniil.core.response.PaymentInfoResponse;
import ru.daniil.core.enums.PaymentStatus;
import ru.daniil.user.repository.order.PaymentInfoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentInfoServiceImpl implements PaymentInfoService{

    private final PaymentInfoRepository paymentInfoRepository;

    public PaymentInfoServiceImpl(PaymentInfoRepository paymentInfoRepository) {
        this.paymentInfoRepository = paymentInfoRepository;
    }

    @Override
    @Transactional
    public PaymentInfoResponse createPayment(PaymentInfo paymentInfo) {

        if (paymentInfoRepository.findByOrderNumber(paymentInfo.getOrderNumber()).isEmpty()) {
            return convertToResponse(paymentInfoRepository.save(paymentInfo));
        } else {
            throw new IllegalArgumentException(
                    String.format("Информация по оплате заказа с номером: %s уже существует",
                            paymentInfo.getOrderNumber())
            );
        }
    }

    @Override
    public Optional<PaymentInfo> getPaymentById(Long id) {
        return paymentInfoRepository.findById(id);
    }

    @Override
    public Page<PaymentInfo> getAllPaymentsOrderedByCreatedAtDesc(Pageable pageable) {
        return paymentInfoRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public PaymentInfo getPaymentByOrderNumber(String orderNumber) {
        return paymentInfoRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Информация по оплате заказа не найдена"));
    }

    @Override
    public List<PaymentInfo> getPaymentsByStatus(String status) {
        return paymentInfoRepository.findByStatus(status);
    }

    @Override
    @Transactional
    public PaymentInfo updatePayment(Long id, PaymentStatus status) {
        PaymentInfo existingPayment = paymentInfoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Оплата с id %d - не найдена", id)
                ));

        existingPayment.setStatus(status);

        return paymentInfoRepository.save(existingPayment);
    }

    @Override
    @Transactional
    public PaymentInfoResponse updatePaymentStatus(PaymentInfo item, String status) {
        item.setStatus(PaymentStatus.valueOf(status));
        return convertToResponse(paymentInfoRepository.save(item));
    }

    @Override
    @Transactional
    public void deletePayment(Long id) {
        if (!paymentInfoRepository.existsById(id)) {
            throw new EntityNotFoundException(
                    String.format("Payment with id %d not found", id)
            );
        }

        paymentInfoRepository.deleteById(id);
    }

    @Override
    public List<PaymentInfo> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentInfoRepository.findByCreatedAtBetween(startDate, endDate);
    }

    private PaymentInfoResponse convertToResponse(PaymentInfo item){
        PaymentInfoResponse result = new PaymentInfoResponse(item.getOrderNumber(), null,
                item.getTotalPrice(), item.getStatus().toString());
        if (item.getMethod() != null) {
            result.setPaymentMethodType(item.getMethod().getType());
        }
        return result;
    }
}
