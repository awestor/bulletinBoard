package ru.daniil.order.orderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.entity.base.discount.OrderDiscount;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.enums.DiscountType;
import ru.daniil.core.response.orderItem.OrderItemsCalculationResult;
import ru.daniil.order.service.order.OrderPriceRecalculationServiceImpl;
import ru.daniil.order.service.order.OrderService;
import ru.daniil.order.service.orderDiscount.OrderDiscountService;
import ru.daniil.order.service.orderItem.OrderItemService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPriceRecalculationServiceImplTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderDiscountService orderDiscountService;

    @Mock
    private OrderItemService orderItemService;

    @InjectMocks
    private OrderPriceRecalculationServiceImpl orderPriceRecalculationService;

    private Order order;
    private Discount discount1;
    private Discount newDiscount;
    private OrderDiscount orderDiscount1;
    private OrderDiscount orderDiscount2;
    private OrderItemsCalculationResult calculationResult;
    private List<OrderItem> updatedItems;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-12345-ABCD");
        order.setTotalPrice(new BigDecimal("1000.00"));

        discount1 = Discount.builder()
                .id(1L)
                .code("SUMMER10")
                .name("Летняя скидка 10%")
                .type(DiscountType.SYSTEM)
                .percentage(new BigDecimal("10.00"))
                .applicableCategoryId(1L)
                .build();

        Discount discount2 = Discount.builder()
                .id(2L)
                .code("WINTER500")
                .name("Зимняя скидка 500р")
                .type(DiscountType.SYSTEM)
                .fixedAmount(new BigDecimal("500.00"))
                .applicableCategoryId(2L)
                .build();

        newDiscount = Discount.builder()
                .id(3L)
                .code("NEW20")
                .name("Новая скидка 20%")
                .type(DiscountType.SYSTEM)
                .percentage(new BigDecimal("20.00"))
                .applicableCategoryId(1L)
                .build();

        orderDiscount1 = new OrderDiscount();
        orderDiscount1.setId(1L);
        orderDiscount1.setOrder(order);
        orderDiscount1.setDiscount(discount1);
        orderDiscount1.setDiscountAmount(new BigDecimal("100.00"));

        orderDiscount2 = new OrderDiscount();
        orderDiscount2.setId(2L);
        orderDiscount2.setOrder(order);
        orderDiscount2.setDiscount(discount2);
        orderDiscount2.setDiscountAmount(new BigDecimal("500.00"));

        updatedItems = List.of(new OrderItem());

        calculationResult = OrderItemsCalculationResult.builder()
                .updatedItems(updatedItems)
                .totalPrice(new BigDecimal("900.00"))
                .build();
    }

    @Test
    void recalculateTotals_WithExistingDiscounts_ShouldRecalculateAndSave() {
        List<OrderDiscount> existingDiscounts = List.of(orderDiscount1, orderDiscount2);

        when(orderDiscountService.getOrderDiscountByOrderId(order.getId()))
                .thenReturn(existingDiscounts);
        when(orderItemService.calculateOrderItemsPrices(eq(order), any()))
                .thenReturn(calculationResult);
        doNothing().when(orderItemService).applyCalculatedPrices(calculationResult);
        doNothing().when(orderService).updateTotalPrice(order);

        orderPriceRecalculationService.recalculateTotals(order);

        assertEquals(calculationResult.getTotalPrice(), order.getTotalPrice());
        verify(orderDiscountService).getOrderDiscountByOrderId(order.getId());
        verify(orderItemService).calculateOrderItemsPrices(eq(order), any());
        verify(orderItemService).applyCalculatedPrices(calculationResult);
        verify(orderService).updateTotalPrice(order);
    }

    @Test
    void recalculateTotals_WithNoDiscounts_ShouldRecalculateWithEmptyMap() {
        when(orderDiscountService.getOrderDiscountByOrderId(order.getId()))
                .thenReturn(List.of());
        when(orderItemService.calculateOrderItemsPrices(eq(order), any()))
                .thenReturn(calculationResult);
        doNothing().when(orderItemService).applyCalculatedPrices(calculationResult);
        doNothing().when(orderService).updateTotalPrice(order);

        orderPriceRecalculationService.recalculateTotals(order);

        assertEquals(calculationResult.getTotalPrice(), order.getTotalPrice());
        verify(orderDiscountService).getOrderDiscountByOrderId(order.getId());
        verify(orderItemService).calculateOrderItemsPrices(eq(order), any());
        verify(orderItemService).applyCalculatedPrices(calculationResult);
        verify(orderService).updateTotalPrice(order);
    }

    @Test
    void recalculateTotals_WhenTotalPriceIsZero_ShouldSaveZero() {
        OrderItemsCalculationResult zeroResult = OrderItemsCalculationResult.builder()
                .updatedItems(updatedItems)
                .totalPrice(BigDecimal.ZERO)
                .build();

        when(orderDiscountService.getOrderDiscountByOrderId(order.getId()))
                .thenReturn(List.of());
        when(orderItemService.calculateOrderItemsPrices(eq(order), any()))
                .thenReturn(zeroResult);
        doNothing().when(orderItemService).applyCalculatedPrices(zeroResult);
        doNothing().when(orderService).updateTotalPrice(order);

        orderPriceRecalculationService.recalculateTotals(order);

        assertEquals(BigDecimal.ZERO, order.getTotalPrice());
        verify(orderService).updateTotalPrice(order);
    }

    @Test
    void previewTotalWithNewDiscount_WhenNewDiscountApplied_ShouldReturnNewTotal() {
        List<OrderDiscount> existingDiscounts = List.of(orderDiscount1);
        BigDecimal expectedTotal = new BigDecimal("800.00");
        OrderItemsCalculationResult previewResult = OrderItemsCalculationResult.builder()
                .updatedItems(updatedItems)
                .totalPrice(expectedTotal)
                .build();

        when(orderDiscountService.getOrderDiscountByOrderId(order.getId()))
                .thenReturn(existingDiscounts);
        when(orderItemService.calculateOrderItemsPrices(eq(order), any()))
                .thenReturn(previewResult);

        BigDecimal result = orderPriceRecalculationService.previewTotalWithNewDiscount(order, newDiscount);

        assertEquals(expectedTotal, result);
        verify(orderDiscountService).getOrderDiscountByOrderId(order.getId());
        verify(orderItemService).calculateOrderItemsPrices(eq(order), any());
        verify(orderItemService, never()).applyCalculatedPrices(any());
        verify(orderService, never()).updateTotalPrice(any());
    }

    @Test
    void previewTotalWithNewDiscount_WithNoExistingDiscounts_ShouldReturnNewTotal() {
        BigDecimal expectedTotal = new BigDecimal("850.00");
        OrderItemsCalculationResult previewResult = OrderItemsCalculationResult.builder()
                .updatedItems(updatedItems)
                .totalPrice(expectedTotal)
                .build();

        when(orderDiscountService.getOrderDiscountByOrderId(order.getId()))
                .thenReturn(List.of());
        when(orderItemService.calculateOrderItemsPrices(eq(order), any()))
                .thenReturn(previewResult);

        BigDecimal result = orderPriceRecalculationService.previewTotalWithNewDiscount(order, newDiscount);

        assertEquals(expectedTotal, result);
        verify(orderDiscountService).getOrderDiscountByOrderId(order.getId());
        verify(orderItemService).calculateOrderItemsPrices(eq(order), any());
        verify(orderItemService, never()).applyCalculatedPrices(any());
        verify(orderService, never()).updateTotalPrice(any());
    }

    @Test
    void previewTotalWithNewDiscount_WhenDiscountHasNullCategory_ShouldThrowNullPointerException() {
        newDiscount.setApplicableCategoryId(null);

        when(orderDiscountService.getOrderDiscountByOrderId(order.getId()))
                .thenReturn(List.of());

        assertThrows(NullPointerException.class,
                () -> orderPriceRecalculationService.previewTotalWithNewDiscount(order, newDiscount));

        verify(orderItemService, never()).calculateOrderItemsPrices(eq(order), any());
    }

    @Test
    void previewTotalWithNewDiscount_ShouldNotModifyOriginalOrder() {
        BigDecimal originalTotal = order.getTotalPrice();

        when(orderDiscountService.getOrderDiscountByOrderId(order.getId()))
                .thenReturn(List.of());
        when(orderItemService.calculateOrderItemsPrices(eq(order), any()))
                .thenReturn(calculationResult);

        orderPriceRecalculationService.previewTotalWithNewDiscount(order, newDiscount);

        assertEquals(originalTotal, order.getTotalPrice());
        verify(orderService, never()).updateTotalPrice(any());
        verify(orderItemService, never()).applyCalculatedPrices(any());
    }

    @Test
    void previewTotalWithNewDiscount_WhenNewDiscountSameAsExisting_ShouldReturnSameTotal() {
        List<OrderDiscount> existingDiscounts = List.of(orderDiscount1);
        Discount sameDiscount = discount1;

        when(orderDiscountService.getOrderDiscountByOrderId(order.getId()))
                .thenReturn(existingDiscounts);
        when(orderItemService.calculateOrderItemsPrices(eq(order), any()))
                .thenReturn(calculationResult);

        BigDecimal result = orderPriceRecalculationService.previewTotalWithNewDiscount(order, sameDiscount);

        assertEquals(calculationResult.getTotalPrice(), result);
    }

    @Test
    void recalculateTotals_ShouldGroupDiscountsByCategoryCorrectly() {
        Discount discountSameCategory = Discount.builder()
                .id(3L)
                .code("ANOTHER10")
                .percentage(new BigDecimal("10.00"))
                .applicableCategoryId(1L)
                .build();

        OrderDiscount orderDiscount3 = new OrderDiscount();
        orderDiscount3.setDiscount(discountSameCategory);

        List<OrderDiscount> existingDiscounts = List.of(orderDiscount1, orderDiscount2, orderDiscount3);

        when(orderDiscountService.getOrderDiscountByOrderId(order.getId()))
                .thenReturn(existingDiscounts);
        when(orderItemService.calculateOrderItemsPrices(eq(order), any()))
                .thenReturn(calculationResult);
        doNothing().when(orderItemService).applyCalculatedPrices(any());
        doNothing().when(orderService).updateTotalPrice(any());

        orderPriceRecalculationService.recalculateTotals(order);

        verify(orderItemService).calculateOrderItemsPrices(eq(order), argThat(map -> {
            List<OrderDiscount> category1Discounts = map.get(1L);
            List<OrderDiscount> category2Discounts = map.get(2L);
            return category1Discounts != null && category1Discounts.size() == 2
                    && category2Discounts != null && category2Discounts.size() == 1;
        }));
    }

    @Test
    void previewTotalWithNewDiscount_WhenExceptionOccurs_ShouldPropagateException() {
        when(orderDiscountService.getOrderDiscountByOrderId(order.getId()))
                .thenReturn(List.of());
        when(orderItemService.calculateOrderItemsPrices(eq(order), any()))
                .thenThrow(new RuntimeException("Calculation error"));

        assertThrows(RuntimeException.class,
                () -> orderPriceRecalculationService.previewTotalWithNewDiscount(order, newDiscount));

        verify(orderItemService, never()).applyCalculatedPrices(any());
        verify(orderService, never()).updateTotalPrice(any());
    }

    @Test
    void recalculateTotals_WhenExceptionOccurs_ShouldPropagateException() {
        when(orderDiscountService.getOrderDiscountByOrderId(order.getId()))
                .thenReturn(List.of());
        when(orderItemService.calculateOrderItemsPrices(eq(order), any()))
                .thenThrow(new RuntimeException("Calculation error"));

        assertThrows(RuntimeException.class,
                () -> orderPriceRecalculationService.recalculateTotals(order));

        verify(orderItemService, never()).applyCalculatedPrices(any());
        verify(orderService, never()).updateTotalPrice(any());
    }
}