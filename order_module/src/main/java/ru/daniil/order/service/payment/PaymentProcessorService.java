package ru.daniil.order.service.payment;

import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.payment.UpdateStatusPaymentRequest;
import ru.daniil.core.response.payment.PaymentCompleteResponse;
import ru.daniil.core.response.payment.PaymentInitiateResponse;

public interface PaymentProcessorService {
    /**
     * Метод отвечающий за инициализация оплаты + заморозка средств и товаров
     * @param user пользователь осуществляющий заказ
     * @return сводка об операции
     */
    PaymentInitiateResponse initiatePayment(User user);

    /**
     * Метод отвечающий за подтверждение оплаты + списание замороженных средств
     * @param request реквест содержащий в себе orderNumber
     * @param user пользователь осуществляющий заказ
     * @return сводка об операции
     */
    PaymentCompleteResponse completePayment(UpdateStatusPaymentRequest request, User user);

    /**
     * Метод отвечающий за отмену оплаты + разморозка средств
     * @param request реквест содержащий в себе orderNumber
     * @param user пользователь осуществляющий заказ
     * @return сводка об операции
     */
    PaymentCompleteResponse cancelPayment(UpdateStatusPaymentRequest request, User user);
}
