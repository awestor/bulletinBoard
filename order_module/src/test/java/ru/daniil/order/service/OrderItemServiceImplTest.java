package ru.daniil.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.order.repository.order.OrderItemRepository;
import ru.daniil.order.service.orderItem.OrderItemServiceImpl;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    private User user;
    private Order order;
    private Product product;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        order = new Order(user);
        order.setId(1L);
        order.setOrderNumber("ORD-123");

        product = new Product();
        product.setId(1L);
        product.setSku("SKU123");
        product.setPrice(new BigDecimal("100.00"));

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        orderItem.setPriceAtTime(new BigDecimal("200.00"));
    }

    @Test
    void createOrderItem_ShouldCreateAndSave() {
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(orderItem);

        OrderItem result = orderItemService.createOrderItem(user, order, product, 2);

        assertNotNull(result);
        assertEquals(order, result.getOrder());
        assertEquals(product, result.getProduct());
        assertEquals(2, result.getQuantity());
        assertEquals(new BigDecimal("100.00"), result.getProduct().getPriceAtTime());
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    void getById_WhenExists_ShouldReturnOrderItem() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));

        OrderItem result = orderItemService.getById(1L);

        assertEquals(orderItem, result);
    }

    @Test
    void getById_WhenNotExists_ShouldThrowException() {
        when(orderItemRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderItemService.getById(999L));

        assertEquals("Указанный товар не найден в заказах", exception.getMessage());
    }

    @Test
    void getByOrderNumber_ShouldReturnItems() {
        List<OrderItem> items = Collections.singletonList(orderItem);
        when(orderItemRepository.findByOrder_OrderNumber("ORD-123")).thenReturn(items);

        List<OrderItem> result = orderItemService.getByOrderNumber("ORD-123");

        assertEquals(1, result.size());
        assertEquals(orderItem, result.getFirst());
    }

    @Test
    void updateItemQuantity_ShouldUpdateQuantity() {
        when(orderItemRepository.updateQuantity(eq(1L), eq(3), any(LocalDateTime.class)))
                .thenReturn(1);

        orderItem.setQuantity(3);
        orderItemService.updateItemQuantity(orderItem);

        verify(orderItemRepository).updateQuantity(eq(1L), eq(3), any(LocalDateTime.class));
    }

    @Test
    void updateItemQuantity_WhenItemNull_ShouldDoNothing() {
        orderItemService.updateItemQuantity(null);
        verify(orderItemRepository, never()).updateQuantity(anyLong(), anyInt(), any());
    }

    @Test
    void updatePriceAtTime_ShouldUpdatePrice() {
        BigDecimal newPrice = new BigDecimal("250.00");
        orderItem.setPriceAtTime(newPrice);
        when(orderItemRepository.updatePrice(1L, newPrice)).thenReturn(1);

        orderItemService.updatePriceAtTime(orderItem);

        verify(orderItemRepository).updatePrice(1L, newPrice);
    }

    @Test
    void validateAvailability_WhenAllItemsAvailable_ShouldReturnTrue() {
        List<OrderItem> items = Collections.singletonList(orderItem);
        when(orderItemRepository.findByOrder_OrderNumber("ORD-123")).thenReturn(items);
        product.setStockQuantity(5);

        boolean result = orderItemService.validateAvailability(order);

        assertTrue(result);
    }

    @Test
    void validateAvailability_WhenItemOutOfStock_ShouldReturnFalse() {
        List<OrderItem> items = Collections.singletonList(orderItem);
        when(orderItemRepository.findByOrder_OrderNumber("ORD-123")).thenReturn(items);
        product.setStockQuantity(1);

        boolean result = orderItemService.validateAvailability(order);

        assertFalse(result);
    }

    @Test
    void validateAvailability_WithMultipleItems_AllAvailable_ShouldReturnTrue() {
        Product product2 = new Product();
        product2.setId(2L);
        product2.setStockQuantity(3);

        OrderItem item2 = new OrderItem();
        item2.setProduct(product2);
        item2.setQuantity(3);

        List<OrderItem> items = Arrays.asList(orderItem, item2);
        when(orderItemRepository.findByOrder_OrderNumber("ORD-123")).thenReturn(items);
        product.setStockQuantity(5);
        product2.setStockQuantity(3);

        boolean result = orderItemService.validateAvailability(order);

        assertTrue(result);
    }

    @Test
    void validateAvailability_WithMultipleItems_OneUnavailable_ShouldReturnFalse() {
        Product product2 = new Product();
        product2.setId(2L);
        product2.setStockQuantity(1);

        OrderItem item2 = new OrderItem();
        item2.setProduct(product2);
        item2.setQuantity(3);

        List<OrderItem> items = Arrays.asList(orderItem, item2);
        when(orderItemRepository.findByOrder_OrderNumber("ORD-123")).thenReturn(items);
        product.setStockQuantity(5);

        boolean result = orderItemService.validateAvailability(order);

        assertFalse(result);
    }

    @Test
    void delete_WhenItemExists_ShouldDelete() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));
        doNothing().when(orderItemRepository).delete(orderItem);

        orderItemService.delete(1L);

        verify(orderItemRepository).delete(orderItem);
    }

    @Test
    void delete_WhenItemNotExists_ShouldThrowException() {
        when(orderItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> orderItemService.delete(999L));
        verify(orderItemRepository, never()).delete(any());
    }
}