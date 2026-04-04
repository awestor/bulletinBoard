package ru.daniil.user.service;

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
import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.CreateOrderItemRequest;
import ru.daniil.core.request.DeleteOrderItemRequest;
import ru.daniil.user.service.order_module.orderItem.order.OrderProcessorServiceImpl;
import ru.daniil.user.service.order_module.orderItem.order.OrderService;
import ru.daniil.user.service.order_module.orderItem.OrderItemService;
import ru.daniil.user.service.product_module.product.ProductService;
import ru.daniil.user.service.user.UserService;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessorServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderProcessorServiceImpl orderProcessorService;

    private User user;
    private Order order;
    private Product product;
    private OrderItem existingItem;
    private CreateOrderItemRequest createRequest;
    private DeleteOrderItemRequest deleteRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        order = new Order(user);
        order.setId(1L);
        order.setOrderNumber("ORD-123");

        Category category = new Category();
        category.setId(1L);

        product = new Product(category, "Test Product", new BigDecimal("100.00"));
        product.setId(1L);
        product.setSku("SKU123");
        product.setStockQuantity(10);
        product.setPrice(new BigDecimal("100.00"));

        existingItem = new OrderItem();
        existingItem.setId(1L);
        existingItem.setProduct(product);
        existingItem.setQuantity(2);
        existingItem.setPriceAtTime(new BigDecimal("200.00"));

        createRequest = new CreateOrderItemRequest();
        createRequest.setSku("SKU123");
        createRequest.setQuantity(3);

        deleteRequest = new DeleteOrderItemRequest();
        deleteRequest.setSku("SKU123");
    }

    @Test
    void addOrderItem_WhenProductExistsAndStockAvailable_ShouldAddNewItem() {
        when(userService.getAuthUser()).thenReturn(user);
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(productService.getBySku("SKU123")).thenReturn(product);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(new ArrayList<>());
        when(orderItemService.createOrderItem(user, order, product, 3)).thenReturn(existingItem);
        doNothing().when(orderService).updateTotalPrice(any(Order.class));

        OrderItem result = orderProcessorService.addOrderItem(createRequest);

        assertNotNull(result);
        verify(orderItemService).createOrderItem(user, order, product, 3);
        verify(orderService).updateTotalPrice(order);
    }

    @Test
    void addOrderItem_WhenItemAlreadyExists_ShouldUpdateQuantity() {
        existingItem.setQuantity(2);
        List<OrderItem> existingItems = Collections.singletonList(existingItem);

        when(userService.getAuthUser()).thenReturn(user);
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(productService.getBySku("SKU123")).thenReturn(product);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(existingItems);
        doNothing().when(orderItemService).updateItemQuantity(any(OrderItem.class));
        doNothing().when(orderService).updateTotalPrice(any(Order.class));

        OrderItem result = orderProcessorService.addOrderItem(createRequest);

        assertEquals(5, result.getQuantity()); // 2 + 3
        verify(orderItemService).updateItemQuantity(result);
        verify(orderItemService, never()).createOrderItem(any(), any(), any(), anyInt());
    }

    @Test
    void addOrderItem_WhenInsufficientStock_ShouldThrowException() {
        product.setStockQuantity(2);

        when(userService.getAuthUser()).thenReturn(user);
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(productService.getBySku("SKU123")).thenReturn(product);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderProcessorService.addOrderItem(createRequest));

        assertTrue(exception.getMessage().contains("превышает его количество"));
        verify(orderItemService, never()).createOrderItem(any(), any(), any(), anyInt());
    }

    @Test
    void addOrderItem_WhenProductNotFound_ShouldThrowException() {
        when(userService.getAuthUser()).thenReturn(user);
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(productService.getBySku("SKU123")).thenThrow(new RuntimeException("Product not found"));

        assertThrows(RuntimeException.class,
                () -> orderProcessorService.addOrderItem(createRequest));
    }

    @Test
    void removeOrderItem_WhenItemExists_ShouldRemoveItem() {
        List<OrderItem> existingItems = Collections.singletonList(existingItem);

        when(userService.getAuthUser()).thenReturn(user);
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(existingItems);
        doNothing().when(orderItemService).delete(1L);
        doNothing().when(orderService).updateTotalPrice(any(Order.class));

        orderProcessorService.removeOrderItem(deleteRequest);

        verify(orderItemService).delete(1L);
        verify(orderService).updateTotalPrice(order);
    }

    @Test
    void removeOrderItem_WhenItemDoesNotExist_ShouldThrowException() {
        when(userService.getAuthUser()).thenReturn(user);
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(new ArrayList<>());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> orderProcessorService.removeOrderItem(deleteRequest));

        assertTrue(exception.getMessage().contains("не найден"));
        verify(orderItemService, never()).delete(anyLong());
    }

    @Test
    void recalculateTotals_WithPercentageDiscount_ShouldApplyDiscount() {
        List<OrderItem> items = Collections.singletonList(existingItem);
        Discount discount = mock(Discount.class);
        OrderDiscount orderDiscount = mock(OrderDiscount.class);

        when(userService.getAuthUser()).thenReturn(user);
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(items);
        when(discount.getPercentage()).thenReturn(new BigDecimal("10.00"));
        when(discount.getApplicableCategoryId()).thenReturn(1L);
        when(orderDiscount.getDiscount()).thenReturn(discount);
        when(productService.getBySku(product.getSku())).thenReturn(product);

        order.getAppliedDiscounts().add(orderDiscount);

        doNothing().when(orderItemService).updatePriceAtTime(any(OrderItem.class));
        doNothing().when(orderService).updateTotalPrice(any(Order.class));

        orderProcessorService.addOrderItem(createRequest);

        verify(orderItemService).updatePriceAtTime(any(OrderItem.class));
        verify(orderService).updateTotalPrice(order);
    }

    @Test
    void recalculateTotals_WithFixedDiscount_ShouldApplyDiscount() {
        List<OrderItem> items = Collections.singletonList(existingItem);
        Discount discount = mock(Discount.class);
        OrderDiscount orderDiscount = mock(OrderDiscount.class);

        when(userService.getAuthUser()).thenReturn(user);
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(items);
        when(discount.getPercentage()).thenReturn(null);
        when(discount.getFixedAmount()).thenReturn(new BigDecimal("50.00"));
        when(discount.getApplicableCategoryId()).thenReturn(1L);
        when(orderDiscount.getDiscount()).thenReturn(discount);
        when(productService.getBySku(product.getSku())).thenReturn(product);

        order.getAppliedDiscounts().add(orderDiscount);

        doNothing().when(orderItemService).updatePriceAtTime(any(OrderItem.class));
        doNothing().when(orderService).updateTotalPrice(any(Order.class));

        orderProcessorService.addOrderItem(createRequest);

        verify(orderItemService).updatePriceAtTime(any(OrderItem.class));
        verify(orderService).updateTotalPrice(order);
    }
}