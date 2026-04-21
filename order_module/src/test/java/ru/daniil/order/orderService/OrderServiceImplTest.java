package ru.daniil.order.orderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.enums.OrderStatus;
import ru.daniil.order.repository.order.OrderRepository;
import ru.daniil.order.service.order.OrderServiceImpl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Order order;
    private Order paidOrder;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setTradingBlocked(false);

        order = new Order(user);
        order.setId(1L);
        order.setOrderNumber("ORD-123");
        order.setStatus(OrderStatus.DRAFT);
        order.setTotalPrice(new BigDecimal("100.00"));

        paidOrder = new Order(user);
        paidOrder.setId(2L);
        paidOrder.setOrderNumber("ORD-456");
        paidOrder.setStatus(OrderStatus.PAID);
    }

    @Test
    void create_WhenUserNotBlocked_ShouldCreateOrder() {
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.create(user);

        assertNotNull(result);
        assertEquals(OrderStatus.DRAFT, result.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void create_WhenUserTradingBlocked_ShouldThrowException() {
        user.setTradingBlocked(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.create(user));

        assertEquals("Торговля заблокирована для данного пользователя", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getById_WhenExists_ShouldReturnOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Order result = orderService.getById(1L);

        assertEquals(order, result);
    }

    @Test
    void getById_WhenNotExists_ShouldThrowException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.getById(999L));

        assertEquals("Заказ не найден", exception.getMessage());
    }

    @Test
    void getByOrderNumber_WhenExists_ShouldReturnOrder() {
        when(orderRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.of(order));

        Order result = orderService.getByOrderNumber("ORD-123");

        assertEquals(order, result);
    }

    @Test
    void getLastOrCreateOrderByUser_WhenNoOrders_ShouldCreateNew() {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(orderRepository.findLastByUserId(1L, pageable)).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.getLastOrCreateOrderByUser(user);

        assertNotNull(result);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void getLastOrCreateOrderByUser_WhenLastOrderIsDraft_ShouldReturnExisting() {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(orderRepository.findLastByUserId(1L, pageable)).thenReturn(Collections.singletonList(order));

        Order result = orderService.getLastOrCreateOrderByUser(user);

        assertEquals(order, result);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getLastOrCreateOrderByUser_WhenLastOrderIsPaid_ShouldCreateNew() {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(orderRepository.findLastByUserId(1L, pageable)).thenReturn(Collections.singletonList(paidOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.getLastOrCreateOrderByUser(user);

        assertNotNull(result);
        assertNotEquals(paidOrder, result);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void getByUser_ShouldReturnUserOrders() {
        List<Order> orders = Arrays.asList(order, paidOrder);
        when(orderRepository.findByUserId(1L)).thenReturn(orders);

        List<Order> result = orderService.getByUser(user);

        assertEquals(2, result.size());
        assertEquals(orders, result);
    }

    @Test
    void updateTotalPrice_ShouldUpdateOrderPrice() {
        orderService.updateTotalPrice(order);

        verify(orderRepository).updateTotalPrice(1L, order.getTotalPrice());
    }

    @Test
    void delete_WhenOrderIsDraft_ShouldDelete() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        doNothing().when(orderRepository).delete(order);

        orderService.delete(1L);

        verify(orderRepository).delete(order);
    }

    @Test
    void delete_WhenOrderIsNotDraft_ShouldThrowException() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(paidOrder));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.delete(2L));

        assertEquals("Завершённые заказы невозможно удалить", exception.getMessage());
        verify(orderRepository, never()).delete(any());
    }
}