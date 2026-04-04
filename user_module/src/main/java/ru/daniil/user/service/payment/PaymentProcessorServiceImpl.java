package ru.daniil.user.service.payment;


import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.payment.PaymentInfo;
import ru.daniil.core.entity.base.payment.PaymentMethod;
import ru.daniil.core.request.CompletePaymentInfoRequest;
import ru.daniil.core.request.CreatePaymentInfoRequest;
import ru.daniil.core.response.PaymentInfoResponse;
import ru.daniil.core.enums.PaymentStatus;
import ru.daniil.user.service.order_module.orderItem.order.OrderService;
import ru.daniil.user.service.order_module.orderItem.OrderItemService;

import java.math.BigDecimal;

@Service
public class PaymentProcessorServiceImpl implements PaymentProcessorService {
    private final PaymentInfoService paymentInfoService;
    private final PaymentMethodService paymentMethodService;
    private final OrderItemService orderItemService;
    private final OrderService orderService;

    public PaymentProcessorServiceImpl(PaymentInfoService paymentInfoService,
                                       PaymentMethodService paymentMethodService, OrderItemService orderItemService, OrderService orderService) {
        this.paymentInfoService = paymentInfoService;
        this.paymentMethodService = paymentMethodService;
        this.orderItemService = orderItemService;
        this.orderService = orderService;
    }

    @Override
    public PaymentInfoResponse createPaymentInfo(CreatePaymentInfoRequest request){
        PaymentMethod paymentMethod = paymentMethodService
                .getPaymentMethodByType(request.getPaymentMethodType());

        PaymentInfo target = new PaymentInfo(request.getOrderNumber(), BigDecimal.ZERO);
        target.setMethod(paymentMethod);

        return paymentInfoService.createPayment(target);
    }

    @Override
    public PaymentInfoResponse completePaymentInfo(CompletePaymentInfoRequest request){
        PaymentMethod paymentMethod = paymentMethodService
                .getPaymentMethodByType(request.getPaymentMethodType());
        PaymentInfo paymentInfo = paymentInfoService
                .getPaymentByOrderNumber(request.getOrderNumber());

        if(paymentInfo.getMethod() != paymentMethod){
            paymentInfoService.updatePaymentStatus(paymentInfo, PaymentStatus.ERROR.toString());;
            throw new IllegalStateException("Метод указанный при начале оплаты заказа и метод" +
                            " указанный при получении подтверждения оплаты отличаются");
        }
        Order order = orderService.getByOrderNumber(request.getOrderNumber());
        if (orderItemService.validateAvailability(order)){
            return paymentInfoService.updatePaymentStatus(paymentInfo, request.getStatus());
        }
        else {
            throw new IllegalStateException("Возникла ошибка с количеством заказываемых товаров.");
        }
    }
}
