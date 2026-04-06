package ru.daniil.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.payment.PaymentInfo;
import ru.daniil.core.entity.base.payment.PaymentMethod;
import ru.daniil.core.request.CompletePaymentInfoRequest;
import ru.daniil.core.request.CreatePaymentInfoRequest;
import ru.daniil.core.response.PaymentInfoResponse;
import ru.daniil.core.enums.PaymentStatus;
import ru.daniil.order.service.order.OrderService;
import ru.daniil.order.service.orderItem.OrderItemService;
import ru.daniil.order.service.payment.PaymentInfoService;
import ru.daniil.order.service.payment.PaymentMethodService;
import ru.daniil.order.service.payment.PaymentProcessorServiceImpl;


import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorServiceImplTest {

    @Mock
    private PaymentInfoService paymentInfoService;

    @Mock
    private PaymentMethodService paymentMethodService;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PaymentProcessorServiceImpl paymentProcessorService;

    private PaymentMethod paymentMethod;
    private PaymentInfo paymentInfo;
    private Order order;
    private CreatePaymentInfoRequest createRequest;
    private CompletePaymentInfoRequest completeRequest;
    private PaymentInfoResponse response;

    @BeforeEach
    void setUp() {
        paymentMethod = new PaymentMethod("CARD", "Credit Card");
        paymentMethod.setId(1L);

        paymentInfo = new PaymentInfo("ORD-123", new BigDecimal("100.00"));
        paymentInfo.setId(1L);
        paymentInfo.setMethod(paymentMethod);

        order = new Order();
        order.setOrderNumber("ORD-123");

        createRequest = new CreatePaymentInfoRequest();
        createRequest.setOrderNumber("ORD-123");
        createRequest.setPaymentMethodType("CARD");

        completeRequest = new CompletePaymentInfoRequest();
        completeRequest.setOrderNumber("ORD-123");
        completeRequest.setPaymentMethodType("CARD");
        completeRequest.setStatus("COMPLETED");

        response = new PaymentInfoResponse
                ("ORD-123", "CARD", new BigDecimal("100.00"), "PROCESSING");
    }

    @Test
    void createPaymentInfo_ShouldCreatePayment() {
        when(paymentMethodService.getPaymentMethodByType("CARD")).thenReturn(paymentMethod);
        when(paymentInfoService.createPayment(any(PaymentInfo.class))).thenReturn(response);

        PaymentInfoResponse result = paymentProcessorService.createPaymentInfo(createRequest);

        assertNotNull(result);
        assertEquals("ORD-123", result.getOrderNumber());
        verify(paymentInfoService).createPayment(any(PaymentInfo.class));
    }

    @Test
    void createPaymentInfo_WhenPaymentMethodNotFound_ShouldThrowException() {
        when(paymentMethodService.getPaymentMethodByType("CARD"))
                .thenThrow(new RuntimeException("Payment method not found"));

        assertThrows(RuntimeException.class,
                () -> paymentProcessorService.createPaymentInfo(createRequest));

        verify(paymentInfoService, never()).createPayment(any());
    }

    @Test
    void completePaymentInfo_WithValidData_ShouldCompletePayment() {
        when(paymentMethodService.getPaymentMethodByType("CARD")).thenReturn(paymentMethod);
        when(paymentInfoService.getPaymentByOrderNumber("ORD-123")).thenReturn(paymentInfo);
        when(orderService.getByOrderNumber("ORD-123")).thenReturn(order);
        when(orderItemService.validateAvailability(order)).thenReturn(true);
        when(paymentInfoService.updatePaymentStatus(paymentInfo, "COMPLETED")).thenReturn(response);

        PaymentInfoResponse result = paymentProcessorService.completePaymentInfo(completeRequest);

        assertNotNull(result);
        verify(paymentInfoService).updatePaymentStatus(paymentInfo, "COMPLETED");
    }

    @Test
    void completePaymentInfo_WhenPaymentMethodMismatch_ShouldThrowException() {
        PaymentMethod differentMethod = new PaymentMethod("PAYPAL", "PayPal");
        paymentInfo.setMethod(differentMethod);

        when(paymentMethodService.getPaymentMethodByType("CARD")).thenReturn(paymentMethod);
        when(paymentInfoService.getPaymentByOrderNumber("ORD-123")).thenReturn(paymentInfo);
        when(paymentInfoService.updatePaymentStatus(paymentInfo, PaymentStatus.ERROR.toString()))
                .thenReturn(response);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> paymentProcessorService.completePaymentInfo(completeRequest));

        assertTrue(exception.getMessage().contains("отличаются"));
        verify(paymentInfoService).updatePaymentStatus(paymentInfo, PaymentStatus.ERROR.toString());
    }

    @Test
    void completePaymentInfo_WhenItemsNotAvailable_ShouldThrowException() {
        when(paymentMethodService.getPaymentMethodByType("CARD")).thenReturn(paymentMethod);
        when(paymentInfoService.getPaymentByOrderNumber("ORD-123")).thenReturn(paymentInfo);
        when(orderService.getByOrderNumber("ORD-123")).thenReturn(order);
        when(orderItemService.validateAvailability(order)).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> paymentProcessorService.completePaymentInfo(completeRequest));

        assertTrue(exception.getMessage().contains("ошибка с количеством"));
        verify(paymentInfoService, never()).updatePaymentStatus(any(), anyString());
    }

    @Test
    void completePaymentInfo_WhenPaymentInfoNotFound_ShouldThrowException() {
        when(paymentMethodService.getPaymentMethodByType("CARD")).thenReturn(paymentMethod);
        when(paymentInfoService.getPaymentByOrderNumber("ORD-123"))
                .thenThrow(new RuntimeException("Payment info not found"));

        assertThrows(RuntimeException.class,
                () -> paymentProcessorService.completePaymentInfo(completeRequest));
    }

    @Test
    void completePaymentInfo_WhenOrderNotFound_ShouldThrowException() {
        when(paymentMethodService.getPaymentMethodByType("CARD")).thenReturn(paymentMethod);
        when(paymentInfoService.getPaymentByOrderNumber("ORD-123")).thenReturn(paymentInfo);
        when(orderService.getByOrderNumber("ORD-123"))
                .thenThrow(new RuntimeException("Order not found"));

        assertThrows(RuntimeException.class,
                () -> paymentProcessorService.completePaymentInfo(completeRequest));
    }
}