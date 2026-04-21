package ru.daniil.order.orderService;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.entity.base.discount.OrderDiscount;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.enums.DiscountType;
import ru.daniil.core.enums.OrderStatus;
import ru.daniil.core.exceptions.DiscountNotApplicableException;
import ru.daniil.core.sharedInterfaces.DiscountProvider;
import ru.daniil.order.service.order.OrderPriceRecalculationService;
import ru.daniil.order.service.order.OrderService;
import ru.daniil.order.service.orderDiscount.OrderDiscountProcessorServiceImpl;
import ru.daniil.order.service.orderDiscount.OrderDiscountService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDiscountProcessorServiceImplTest {

    @Mock
    private OrderService orderService;

    @Mock
    private DiscountProvider discountProvider;

    @Mock
    private OrderDiscountService orderDiscountService;

    @Mock
    private OrderPriceRecalculationService orderPriceRecalculationService;

    @InjectMocks
    private OrderDiscountProcessorServiceImpl orderDiscountProcessorService;

    private User user;
    private Order order;
    private Discount discount;
    private OrderDiscount orderDiscount;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setLogin("testuser");

        order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-12345-ABCD");
        order.setStatus(OrderStatus.DRAFT);
        order.setUser(user);
        order.setTotalPrice(new BigDecimal("1000.00"));

        discount = Discount.builder()
                .id(1L)
                .code("SUMMER2024")
                .name("Летняя скидка")
                .type(DiscountType.SYSTEM)
                .percentage(new BigDecimal("10.00"))
                .applicableCategoryId(1L)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(100)
                .usageCount(0)
                .build();

        orderDiscount = OrderDiscount.builder()
                .id(1L)
                .order(order)
                .discount(discount)
                .discountAmount(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void applyDiscountToOrder_WhenValid_ShouldApplyAndReturnOrderDiscount() {
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(discountProvider.getActiveDiscountByCode("SUMMER2024")).thenReturn(discount);
        when(orderDiscountService.getOrderDiscountByIds(order.getId(), discount.getId()))
                .thenReturn(Optional.empty());
        when(orderPriceRecalculationService.previewTotalWithNewDiscount(order, discount))
                .thenReturn(new BigDecimal("900.00"));
        when(orderDiscountService.save(any(OrderDiscount.class))).thenReturn(orderDiscount);

        OrderDiscount result = orderDiscountProcessorService.applyDiscountToOrder(
                "ORD-12345-ABCD", "SUMMER2024", user);

        assertNotNull(result);
        assertEquals(orderDiscount.getId(), result.getId());
        verify(discountProvider).incrementUsageCount(discount.getId());
        verify(orderDiscountService).save(any(OrderDiscount.class));
        verify(orderPriceRecalculationService).recalculateTotals(order);
        verify(discountProvider, never()).decrementUsageCount(any());
    }

    @Test
    void applyDiscountToOrder_WhenOrderNotFound_ShouldThrowIllegalArgumentException() {
        Order otherOrder = new Order();
        otherOrder.setOrderNumber("ORD-OTHER");
        otherOrder.setStatus(OrderStatus.DRAFT);

        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(otherOrder);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderDiscountProcessorService.applyDiscountToOrder(
                        "ORD-12345-ABCD", "SUMMER2024", user));

        assertEquals("Заказ с указанным номером не найден или недоступен", exception.getMessage());
        verify(discountProvider, never()).getActiveDiscountByCode(any());
    }

    @Test
    void applyDiscountToOrder_WhenOrderNotDraft_ShouldThrowIllegalArgumentException() {
        order.setStatus(OrderStatus.PAID);
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderDiscountProcessorService.applyDiscountToOrder(
                        "ORD-12345-ABCD", "SUMMER2024", user));

        assertEquals("Заказ с указанным номером не найден или недоступен", exception.getMessage());
        verify(discountProvider, never()).getActiveDiscountByCode(any());
    }

    @Test
    void applyDiscountToOrder_WhenDiscountAlreadyApplied_ShouldThrowIllegalArgumentException() {
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(discountProvider.getActiveDiscountByCode("SUMMER2024")).thenReturn(discount);
        when(orderDiscountService.getOrderDiscountByIds(order.getId(), discount.getId()))
                .thenReturn(Optional.of(orderDiscount));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderDiscountProcessorService.applyDiscountToOrder(
                        "ORD-12345-ABCD", "SUMMER2024", user));

        assertEquals("Эта скидка уже применена к заказу", exception.getMessage());
        verify(discountProvider, never()).incrementUsageCount(any());
        verify(orderDiscountService, never()).save(any());
    }

    @Test
    void applyDiscountToOrder_WhenDiscountDoesNotAffectTotal_ShouldThrowDiscountNotApplicableException() {
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(discountProvider.getActiveDiscountByCode("SUMMER2024")).thenReturn(discount);
        when(orderDiscountService.getOrderDiscountByIds(order.getId(), discount.getId()))
                .thenReturn(Optional.empty());
        doNothing().when(discountProvider).incrementUsageCount(discount.getId());
        when(orderPriceRecalculationService.previewTotalWithNewDiscount(order, discount))
                .thenReturn(new BigDecimal("1000.00"));
        doNothing().when(discountProvider).decrementUsageCount(discount.getId());

        DiscountNotApplicableException exception = assertThrows(DiscountNotApplicableException.class,
                () -> orderDiscountProcessorService.applyDiscountToOrder(
                        "ORD-12345-ABCD", "SUMMER2024", user));

        assertTrue(exception.getMessage().contains("Эта скидка не влияет на итоговую сумму заказа"));
        verify(discountProvider).incrementUsageCount(discount.getId());
        verify(discountProvider).decrementUsageCount(discount.getId());
        verify(orderDiscountService, never()).save(any());
        verify(orderPriceRecalculationService, never()).recalculateTotals(any());
    }

    @Test
    void applyDiscountToOrder_WhenExceptionOccurs_ShouldDecrementUsageCount() {
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(discountProvider.getActiveDiscountByCode("SUMMER2024")).thenReturn(discount);
        when(orderDiscountService.getOrderDiscountByIds(order.getId(), discount.getId()))
                .thenReturn(Optional.empty());
        doNothing().when(discountProvider).incrementUsageCount(discount.getId());
        when(orderPriceRecalculationService.previewTotalWithNewDiscount(order, discount))
                .thenThrow(new RuntimeException("Calculation error"));
        doNothing().when(discountProvider).decrementUsageCount(discount.getId());

        assertThrows(RuntimeException.class,
                () -> orderDiscountProcessorService.applyDiscountToOrder(
                        "ORD-12345-ABCD", "SUMMER2024", user));

        verify(discountProvider).incrementUsageCount(discount.getId());
        verify(discountProvider).decrementUsageCount(discount.getId());
        verify(orderDiscountService, never()).save(any());
    }

    @Test
    void removeDiscountFromOrder_WhenValid_ShouldDeleteDiscount() {
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(discountProvider.getActiveDiscountByCode("SUMMER2024")).thenReturn(discount);
        doNothing().when(orderDiscountService).deleteByIds(order.getId(), discount.getId());

        orderDiscountProcessorService.removeDiscountFromOrder(
                "ORD-12345-ABCD", "SUMMER2024", user);

        verify(orderDiscountService).deleteByIds(order.getId(), discount.getId());
    }

    @Test
    void removeDiscountFromOrder_WhenOrderNotFound_ShouldThrowIllegalArgumentException() {
        Order otherOrder = new Order();
        otherOrder.setOrderNumber("ORD-OTHER");
        otherOrder.setStatus(OrderStatus.DRAFT);

        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(otherOrder);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderDiscountProcessorService.removeDiscountFromOrder(
                        "ORD-12345-ABCD", "SUMMER2024", user));

        assertEquals("Заказ с указанным номером не найден или недоступен", exception.getMessage());
        verify(orderDiscountService, never()).deleteByIds(any(), any());
    }

    @Test
    void removeDiscountFromOrder_WhenOrderNotDraft_ShouldThrowIllegalArgumentException() {
        order.setStatus(OrderStatus.PAID);
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderDiscountProcessorService.removeDiscountFromOrder(
                        "ORD-12345-ABCD", "SUMMER2024", user));

        assertEquals("Заказ с указанным номером не найден или недоступен", exception.getMessage());
        verify(orderDiscountService, never()).deleteByIds(any(), any());
    }

    @Test
    void getAppliedDiscounts_WhenValid_ShouldReturnList() {
        List<OrderDiscount> expectedDiscounts = List.of(orderDiscount);

        when(orderService.getByOrderNumber("ORD-12345-ABCD")).thenReturn(order);
        when(orderDiscountService.getOrderDiscountByOrderId(order.getId()))
                .thenReturn(expectedDiscounts);

        List<OrderDiscount> result = orderDiscountProcessorService.getAppliedDiscounts(
                "ORD-12345-ABCD", user);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(orderDiscount.getId(), result.getFirst().getId());
        verify(orderService).getByOrderNumber("ORD-12345-ABCD");
        verify(orderDiscountService).getOrderDiscountByOrderId(order.getId());
    }

    @Test
    void getAppliedDiscounts_WhenUserNotOwner_ShouldThrowIllegalArgumentException() {
        User otherUser = new User();
        otherUser.setEmail("other@example.com");
        order.setUser(otherUser);

        when(orderService.getByOrderNumber("ORD-12345-ABCD")).thenReturn(order);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderDiscountProcessorService.getAppliedDiscounts(
                        "ORD-12345-ABCD", user));

        assertEquals("Номер заказа не соответствует ни одному из заказов пользователя", exception.getMessage());
        verify(orderDiscountService, never()).getOrderDiscountByOrderId(any());
    }

    @Test
    void getAppliedDiscounts_WhenOrderNotFound_ShouldThrowEntityNotFoundException() {
        when(orderService.getByOrderNumber("ORD-12345-ABCD"))
                .thenThrow(new EntityNotFoundException("Заказ не найден"));

        assertThrows(EntityNotFoundException.class,
                () -> orderDiscountProcessorService.getAppliedDiscounts(
                        "ORD-12345-ABCD", user));
    }
}