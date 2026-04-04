package ru.daniil.user.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.daniil.core.entity.base.payment.PaymentInfo;
import ru.daniil.core.entity.base.payment.PaymentMethod;
import ru.daniil.core.response.PaymentInfoResponse;
import ru.daniil.core.enums.PaymentStatus;
import ru.daniil.user.repository.order.PaymentInfoRepository;
import ru.daniil.user.service.payment.PaymentInfoServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentInfoServiceImplTest {

    @Mock
    private PaymentInfoRepository paymentInfoRepository;

    @InjectMocks
    private PaymentInfoServiceImpl paymentInfoService;

    private PaymentInfo paymentInfo;

    @BeforeEach
    void setUp() {
        PaymentMethod paymentMethod = new PaymentMethod("CARD", "Credit Card");
        paymentMethod.setId(1L);

        paymentInfo = new PaymentInfo("ORD-123", new BigDecimal("100.00"));
        paymentInfo.setId(1L);
        paymentInfo.setMethod(paymentMethod);
        paymentInfo.setStatus(PaymentStatus.PROCESSING);
    }

    @Test
    void createPayment_WhenNotExists_ShouldCreate() {
        when(paymentInfoRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.empty());
        when(paymentInfoRepository.save(any(PaymentInfo.class))).thenReturn(paymentInfo);

        PaymentInfoResponse result = paymentInfoService.createPayment(paymentInfo);

        assertNotNull(result);
        assertEquals("ORD-123", result.getOrderNumber());
        assertEquals("CARD", result.getPaymentMethodType());
        verify(paymentInfoRepository).save(paymentInfo);
    }

    @Test
    void createPayment_WhenAlreadyExists_ShouldThrowException() {
        when(paymentInfoRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.of(paymentInfo));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> paymentInfoService.createPayment(paymentInfo));

        assertTrue(exception.getMessage().contains("уже существует"));
        verify(paymentInfoRepository, never()).save(any());
    }

    @Test
    void createPayment_WithoutPaymentMethod_ShouldCreateWithoutMethod() {
        paymentInfo.setMethod(null);
        when(paymentInfoRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.empty());
        when(paymentInfoRepository.save(any(PaymentInfo.class))).thenReturn(paymentInfo);

        PaymentInfoResponse result = paymentInfoService.createPayment(paymentInfo);

        assertNotNull(result);
        assertNull(result.getPaymentMethodType());
    }

    @Test
    void getPaymentById_WhenExists_ShouldReturnOptional() {
        when(paymentInfoRepository.findById(1L)).thenReturn(Optional.of(paymentInfo));

        Optional<PaymentInfo> result = paymentInfoService.getPaymentById(1L);

        assertTrue(result.isPresent());
        assertEquals(paymentInfo, result.get());
    }

    @Test
    void getPaymentById_WhenNotExists_ShouldReturnEmptyOptional() {
        when(paymentInfoRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<PaymentInfo> result = paymentInfoService.getPaymentById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllPaymentsOrderedByCreatedAtDesc_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        List<PaymentInfo> payments = Arrays.asList(paymentInfo);
        Page<PaymentInfo> expectedPage = new PageImpl<>(payments, pageable, 1);

        when(paymentInfoRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(expectedPage);

        Page<PaymentInfo> result = paymentInfoService.getAllPaymentsOrderedByCreatedAtDesc(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(paymentInfo, result.getContent().get(0));
    }

    @Test
    void getPaymentByOrderNumber_WhenExists_ShouldReturnPayment() {
        when(paymentInfoRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.of(paymentInfo));

        PaymentInfo result = paymentInfoService.getPaymentByOrderNumber("ORD-123");

        assertEquals(paymentInfo, result);
    }

    @Test
    void getPaymentByOrderNumber_WhenNotExists_ShouldThrowException() {
        when(paymentInfoRepository.findByOrderNumber("ORD-999")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentInfoService.getPaymentByOrderNumber("ORD-999"));

        assertTrue(exception.getMessage().contains("не найдена"));
    }

    @Test
    void getPaymentsByStatus_ShouldReturnFilteredPayments() {
        List<PaymentInfo> payments = Arrays.asList(paymentInfo);
        when(paymentInfoRepository.findByStatus(PaymentStatus.PROCESSING.toString())).thenReturn(payments);

        List<PaymentInfo> result = paymentInfoService.getPaymentsByStatus(PaymentStatus.PROCESSING.toString());

        assertEquals(1, result.size());
        assertEquals(paymentInfo, result.get(0));
    }

    @Test
    void updatePayment_ShouldUpdateStatus() {
        when(paymentInfoRepository.findById(1L)).thenReturn(Optional.of(paymentInfo));
        when(paymentInfoRepository.save(any(PaymentInfo.class))).thenReturn(paymentInfo);

        PaymentInfo result = paymentInfoService.updatePayment(1L, PaymentStatus.COMPLETED);

        assertEquals(PaymentStatus.COMPLETED.toString(), result.getStatus().toString());
        verify(paymentInfoRepository).save(paymentInfo);
    }

    @Test
    void updatePayment_WhenNotFound_ShouldThrowException() {
        when(paymentInfoRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentInfoService.updatePayment(999L, PaymentStatus.COMPLETED));

        assertTrue(exception.getMessage().contains("не найдена"));
        verify(paymentInfoRepository, never()).save(any());
    }

    @Test
    void updatePaymentStatus_ShouldUpdateAndReturnResponse() {
        when(paymentInfoRepository.save(any(PaymentInfo.class))).thenReturn(paymentInfo);

        PaymentInfoResponse result = paymentInfoService.updatePaymentStatus(paymentInfo, "COMPLETED");

        assertEquals("COMPLETED", paymentInfo.getStatus().toString());
        assertEquals("COMPLETED", result.getStatus());
        verify(paymentInfoRepository).save(paymentInfo);
    }

    @Test
    void deletePayment_WhenExists_ShouldDelete() {
        when(paymentInfoRepository.existsById(1L)).thenReturn(true);
        doNothing().when(paymentInfoRepository).deleteById(1L);

        paymentInfoService.deletePayment(1L);

        verify(paymentInfoRepository).deleteById(1L);
    }

    @Test
    void deletePayment_WhenNotExists_ShouldThrowException() {
        when(paymentInfoRepository.existsById(999L)).thenReturn(false);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentInfoService.deletePayment(999L));

        assertTrue(exception.getMessage().contains("not found"));
        verify(paymentInfoRepository, never()).deleteById(anyLong());
    }

    @Test
    void getPaymentsByDateRange_ShouldReturnFilteredPayments() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<PaymentInfo> payments = Arrays.asList(paymentInfo);

        when(paymentInfoRepository.findByCreatedAtBetween(start, end)).thenReturn(payments);

        List<PaymentInfo> result = paymentInfoService.getPaymentsByDateRange(start, end);

        assertEquals(1, result.size());
        assertEquals(paymentInfo, result.get(0));
    }

    @Test
    void getPaymentsByDateRange_WithNoPayments_ShouldReturnEmptyList() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        when(paymentInfoRepository.findByCreatedAtBetween(start, end)).thenReturn(Arrays.asList());

        List<PaymentInfo> result = paymentInfoService.getPaymentsByDateRange(start, end);

        assertTrue(result.isEmpty());
    }
}