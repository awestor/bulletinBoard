package ru.daniil.bulletinBoard.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.daniil.bulletinBoard.entity.base.payment.PaymentMethod;
import ru.daniil.bulletinBoard.entity.request.CreatePaymentMethodRequest;
import ru.daniil.bulletinBoard.entity.response.PaymentMethodResponse;
import ru.daniil.bulletinBoard.repository.order.PaymentMethodRepository;
import ru.daniil.bulletinBoard.service.payment.PaymentMethodServiceImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceImplTest {

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @InjectMocks
    private PaymentMethodServiceImpl paymentMethodService;

    private PaymentMethod cardMethod;
    private PaymentMethod paypalMethod;
    private CreatePaymentMethodRequest updateRequest;

    @BeforeEach
    void setUp() {
        cardMethod = new PaymentMethod("CARD", "Credit Card");
        cardMethod.setId(1L);

        paypalMethod = new PaymentMethod("PAYPAL", "PayPal");
        paypalMethod.setId(2L);

        updateRequest = new CreatePaymentMethodRequest();
        updateRequest.setType("NEW_CARD");
        updateRequest.setDescription("New Card Description");
    }

    @Test
    void getPaymentMethodByType_WhenExists_ShouldReturnMethod() {
        when(paymentMethodRepository.findByType("CARD")).thenReturn(Optional.of(cardMethod));

        PaymentMethod result = paymentMethodService.getPaymentMethodByType("CARD");

        assertEquals(cardMethod, result);
    }

    @Test
    void getPaymentMethodByType_WhenNotExists_ShouldThrowException() {
        when(paymentMethodRepository.findByType("UNKNOWN")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentMethodService.getPaymentMethodByType("UNKNOWN"));

        assertTrue(exception.getMessage().contains("not found with type: UNKNOWN"));
    }

    @Test
    void getPaymentMethodResponseByType_ShouldReturnResponse() {
        when(paymentMethodRepository.findByType("CARD")).thenReturn(Optional.of(cardMethod));

        PaymentMethodResponse result = paymentMethodService.getPaymentMethodResponseByType("CARD");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("CARD", result.getType());
    }

    @Test
    void getAllPaymentMethods_ShouldReturnAllMethods() {
        List<PaymentMethod> methods = Arrays.asList(cardMethod, paypalMethod);
        when(paymentMethodRepository.findAll()).thenReturn(methods);

        List<PaymentMethodResponse> result = paymentMethodService.getAllPaymentMethods();

        assertEquals(2, result.size());
        assertEquals("CARD", result.get(0).getType());
        assertEquals("PAYPAL", result.get(1).getType());
    }

    @Test
    void getAllPaymentMethods_WhenEmpty_ShouldReturnEmptyList() {
        when(paymentMethodRepository.findAll()).thenReturn(List.of());

        List<PaymentMethodResponse> result = paymentMethodService.getAllPaymentMethods();

        assertTrue(result.isEmpty());
    }

    @Test
    void updatePaymentMethod_WithNewType_ShouldUpdate() {
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(cardMethod));
        when(paymentMethodRepository.existsByType("NEW_CARD")).thenReturn(false);
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(cardMethod);

        PaymentMethodResponse result = paymentMethodService.updatePaymentMethod(1L, updateRequest);

        assertEquals("NEW_CARD", cardMethod.getType());
        assertEquals("New Card Description", cardMethod.getDescription());
        verify(paymentMethodRepository).save(cardMethod);
    }

    @Test
    void updatePaymentMethod_WithSameType_ShouldUpdateDescriptionOnly() {
        updateRequest.setType("CARD");

        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(cardMethod));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(cardMethod);

        PaymentMethodResponse result = paymentMethodService.updatePaymentMethod(1L, updateRequest);

        assertEquals("CARD", cardMethod.getType());
        assertEquals("New Card Description", cardMethod.getDescription());
        verify(paymentMethodRepository, never()).existsByType(anyString());
        verify(paymentMethodRepository).save(cardMethod);
    }

    @Test
    void updatePaymentMethod_WhenTypeExists_ShouldThrowException() {
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(cardMethod));
        when(paymentMethodRepository.existsByType("NEW_CARD")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> paymentMethodService.updatePaymentMethod(1L, updateRequest));

        assertTrue(exception.getMessage().contains("уже существует"));
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    void updatePaymentMethod_WhenNotFound_ShouldThrowException() {
        when(paymentMethodRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentMethodService.updatePaymentMethod(999L, updateRequest));

        assertTrue(exception.getMessage().contains("не найден"));
    }

    @Test
    void deletePaymentMethodById_WhenExists_ShouldDelete() {
        when(paymentMethodRepository.existsById(1L)).thenReturn(true);
        doNothing().when(paymentMethodRepository).deleteById(1L);

        paymentMethodService.deletePaymentMethodById(1L);

        verify(paymentMethodRepository).deleteById(1L);
    }

    @Test
    void deletePaymentMethodById_WhenNotExists_ShouldThrowException() {
        when(paymentMethodRepository.existsById(999L)).thenReturn(false);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentMethodService.deletePaymentMethodById(999L));

        assertTrue(exception.getMessage().contains("не найден"));
        verify(paymentMethodRepository, never()).deleteById(anyLong());
    }

    @Test
    void deletePaymentMethodByType_WhenExists_ShouldDelete() {
        when(paymentMethodRepository.findByType("CARD")).thenReturn(Optional.of(cardMethod));
        doNothing().when(paymentMethodRepository).delete(cardMethod);

        paymentMethodService.deletePaymentMethodByType("CARD");

        verify(paymentMethodRepository).delete(cardMethod);
    }

    @Test
    void deletePaymentMethodByType_WhenNotExists_ShouldThrowException() {
        when(paymentMethodRepository.findByType("UNKNOWN")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentMethodService.deletePaymentMethodByType("UNKNOWN"));

        assertTrue(exception.getMessage().contains("не найден"));
        verify(paymentMethodRepository, never()).delete(any());
    }
}