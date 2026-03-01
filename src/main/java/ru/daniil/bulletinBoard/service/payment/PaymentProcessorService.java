package ru.daniil.bulletinBoard.service.payment;

import ru.daniil.bulletinBoard.entity.request.CompletePaymentInfoRequest;
import ru.daniil.bulletinBoard.entity.request.CreatePaymentInfoRequest;
import ru.daniil.bulletinBoard.entity.response.PaymentInfoResponse;

public interface PaymentProcessorService {
    PaymentInfoResponse createPaymentInfo(CreatePaymentInfoRequest request);

    PaymentInfoResponse completePaymentInfo(CompletePaymentInfoRequest request);
}
