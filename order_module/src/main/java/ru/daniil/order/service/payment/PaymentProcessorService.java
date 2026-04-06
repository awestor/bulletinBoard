package ru.daniil.order.service.payment;

import ru.daniil.core.request.CompletePaymentInfoRequest;
import ru.daniil.core.request.CreatePaymentInfoRequest;
import ru.daniil.core.response.PaymentInfoResponse;

public interface PaymentProcessorService {
    PaymentInfoResponse createPaymentInfo(CreatePaymentInfoRequest request);

    PaymentInfoResponse completePaymentInfo(CompletePaymentInfoRequest request);
}
