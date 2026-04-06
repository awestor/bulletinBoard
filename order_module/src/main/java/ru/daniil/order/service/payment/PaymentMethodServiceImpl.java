package ru.daniil.order.service.payment;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.payment.PaymentMethod;
import ru.daniil.core.request.CreatePaymentMethodRequest;
import ru.daniil.core.response.PaymentMethodResponse;
import ru.daniil.order.repository.payment.PaymentMethodRepository;


import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentMethodServiceImpl implements PaymentMethodService {
    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodServiceImpl(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public PaymentMethod getPaymentMethodByType(String type) {

        return paymentMethodRepository.findByType(type)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Payment method not found with type: %s", type)
                ));
    }

    public PaymentMethodResponse getPaymentMethodResponseByType(String type) {
        PaymentMethod target = getPaymentMethodByType(type);

        return new PaymentMethodResponse(target.getId(), target.getType());
    }

    public List<PaymentMethodResponse> getAllPaymentMethods() {
        List<PaymentMethod> data = (List<PaymentMethod>) paymentMethodRepository.findAll();
        List<PaymentMethodResponse> result = new ArrayList<PaymentMethodResponse>();
        for(PaymentMethod item : data){
            result.add(convertToResponse(item));
        }
        return result;
    }

    @Transactional
    public PaymentMethodResponse updatePaymentMethod(Long id, CreatePaymentMethodRequest request) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Способ оплаты с id: %d - не найден", id)
                ));

        if (!paymentMethod.getType().equals(request.getType())) {
            if (paymentMethodRepository.existsByType(request.getType())) {
                throw new IllegalArgumentException(
                        String.format("Способ оплаты с типом '%s' уже существует", request.getType())
                );
            }
            paymentMethod.setType(request.getType());
        }

        paymentMethod.setDescription(request.getDescription());
        PaymentMethod updatedPaymentMethod = paymentMethodRepository.save(paymentMethod);

        return convertToResponse(updatedPaymentMethod);
    }

    @Transactional
    public void deletePaymentMethodById(Long id) {
        if (!paymentMethodRepository.existsById(id)) {
            throw new EntityNotFoundException(
                    String.format("Способ оплаты с id: %d - не найден", id)
            );
        }

        paymentMethodRepository.deleteById(id);
    }

    @Transactional
    public void deletePaymentMethodByType(String type) {
        PaymentMethod target = paymentMethodRepository.findByType(type)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Способ оплаты с типом: %s - не найден", type)
                )
        );

        paymentMethodRepository.delete(target);
    }

    private PaymentMethodResponse convertToResponse(PaymentMethod item){
        return new PaymentMethodResponse(item.getId(), item.getType());
    }
}
