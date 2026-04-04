package ru.daniil.user.service.payment;

import ru.daniil.core.entity.base.payment.PaymentMethod;
import ru.daniil.core.request.CreatePaymentMethodRequest;
import ru.daniil.core.response.PaymentMethodResponse;

import java.util.List;

public interface PaymentMethodService {
    PaymentMethod getPaymentMethodByType(String type);

    PaymentMethodResponse getPaymentMethodResponseByType(String type);

    List<PaymentMethodResponse> getAllPaymentMethods();

    PaymentMethodResponse updatePaymentMethod(Long id, CreatePaymentMethodRequest request);

    void deletePaymentMethodById(Long id);

    void deletePaymentMethodByType(String type);
}
