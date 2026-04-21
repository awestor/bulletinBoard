package ru.daniil.order.orderService;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.orderItem.CreateOrderItemRequest;
import ru.daniil.core.request.orderItem.DeleteOrderItemRequest;
import ru.daniil.core.request.orderItem.ReduceQuantityRequest;
import ru.daniil.order.service.order.OrderPriceRecalculationServiceImpl;
import ru.daniil.order.service.order.OrderProcessorServiceImpl;
import ru.daniil.order.service.order.OrderService;
import ru.daniil.order.service.orderItem.OrderItemService;
import ru.daniil.product.service.product.ProductService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessorServiceImplTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private ProductService productService;

    @Mock
    private OrderPriceRecalculationServiceImpl orderPriceRecalculationService;

    @InjectMocks
    private OrderProcessorServiceImpl orderProcessorService;

    private User user;
    private Order order;
    private Product product;
    private OrderItem existingItem;
    private CreateOrderItemRequest createRequest;
    private DeleteOrderItemRequest deleteRequest;
    private ReduceQuantityRequest reduceRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        order = new Order(user);
        order.setId(1L);
        order.setOrderNumber("ORD-123");

        Category category = new Category();
        category.setId(1L);

        product = new Product(category, user, "Test Product", new BigDecimal("100.00"));
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

        reduceRequest = new ReduceQuantityRequest();
        reduceRequest.setSku("SKU123");
        reduceRequest.setQuantity(1);
    }

    @Test
    void addOrderItem_WhenProductExistsAndStockAvailable_ShouldAddNewItem() {
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(productService.getBySku("SKU123")).thenReturn(product);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(new ArrayList<>());
        when(orderItemService.createOrderItem(user, order, product, 3)).thenReturn(existingItem);
        doNothing().when(orderPriceRecalculationService).recalculateTotals(order);

        OrderItem result = orderProcessorService.addOrderItem(createRequest, user);

        assertNotNull(result);
        verify(orderItemService).createOrderItem(user, order, product, 3);
        verify(orderPriceRecalculationService).recalculateTotals(order);
    }

    @Test
    void addOrderItem_WhenItemAlreadyExists_ShouldUpdateQuantity() {
        existingItem.setQuantity(2);
        List<OrderItem> existingItems = Collections.singletonList(existingItem);

        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(productService.getBySku("SKU123")).thenReturn(product);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(existingItems);
        doNothing().when(orderPriceRecalculationService).recalculateTotals(order);

        OrderItem result = orderProcessorService.addOrderItem(createRequest, user);

        assertEquals(5, result.getQuantity()); // 2 + 3
        verify(orderItemService).updateItemQuantity(result);
        verify(orderItemService, never()).createOrderItem(any(), any(), any(), anyInt());
    }

    @Test
    void addOrderItem_WhenInsufficientStock_ShouldThrowException() {
        product.setStockQuantity(2);

        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(productService.getBySku("SKU123")).thenReturn(product);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderProcessorService.addOrderItem(createRequest, user));

        assertTrue(exception.getMessage().contains("превышает его количество"));
        verify(orderItemService, never()).createOrderItem(any(), any(), any(), anyInt());
    }

    @Test
    void addOrderItem_WhenProductNotFound_ShouldThrowException() {
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(productService.getBySku("SKU123")).thenThrow(new RuntimeException("Product not found"));

        assertThrows(RuntimeException.class,
                () -> orderProcessorService.addOrderItem(createRequest, user));
    }

    @Test
    void removeOrderItem_WhenItemExists_ShouldRemoveItem() {
        List<OrderItem> existingItems = Collections.singletonList(existingItem);

        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(existingItems);
        doNothing().when(orderItemService).delete(1L);
        doNothing().when(orderPriceRecalculationService).recalculateTotals(order);

        orderProcessorService.removeOrderItem(deleteRequest, user);

        verify(orderItemService).delete(1L);
        verify(orderPriceRecalculationService).recalculateTotals(order);
    }

    @Test
    void removeOrderItem_WhenItemDoesNotExist_ShouldThrowException() {
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(new ArrayList<>());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> orderProcessorService.removeOrderItem(deleteRequest, user));

        assertTrue(exception.getMessage().contains("не найден"));
        verify(orderItemService, never()).delete(anyLong());
    }

    @Test
    void reduceQuantityOrderItem_WhenItemExists_ShouldReduceQuantity() {
        existingItem.setQuantity(5);
        List<OrderItem> existingItems = Collections.singletonList(existingItem);

        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(existingItems);
        doNothing().when(orderItemService).updateItemQuantity(any(OrderItem.class));
        doNothing().when(orderPriceRecalculationService).recalculateTotals(order);

        orderProcessorService.reduceQuantityOrderItem(reduceRequest, user);

        assertEquals(4, existingItem.getQuantity()); // 5 - 1
        verify(orderPriceRecalculationService).recalculateTotals(order);
    }
}