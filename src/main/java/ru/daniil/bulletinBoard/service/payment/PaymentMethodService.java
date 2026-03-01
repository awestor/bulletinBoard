package ru.daniil.bulletinBoard.service.payment;

import ru.daniil.bulletinBoard.entity.base.payment.PaymentMethod;
import ru.daniil.bulletinBoard.entity.request.CreatePaymentMethodRequest;
import ru.daniil.bulletinBoard.entity.response.PaymentMethodResponse;

import java.util.List;

public interface PaymentMethodService {
    PaymentMethod getPaymentMethodByType(String type);

    PaymentMethodResponse getPaymentMethodResponseByType(String type);

    List<PaymentMethodResponse> getAllPaymentMethods();

    PaymentMethodResponse updatePaymentMethod(Long id, CreatePaymentMethodRequest request);

    void deletePaymentMethodById(Long id);

    void deletePaymentMethodByType(String type);
}
