package ru.daniil.order.orderService;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
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
import ru.daniil.core.entity.base.wallet.Wallet;
import ru.daniil.core.entity.base.wallet.WalletTransaction;
import ru.daniil.core.enums.OrderStatus;
import ru.daniil.core.enums.wallet.TransactionStatus;
import ru.daniil.core.enums.wallet.TransactionType;
import ru.daniil.core.request.payment.UpdateStatusPaymentRequest;
import ru.daniil.core.response.payment.PaymentCompleteResponse;
import ru.daniil.core.response.payment.PaymentInitiateResponse;
import ru.daniil.order.mapper.PaymentMapper;
import ru.daniil.order.service.order.OrderService;
import ru.daniil.order.service.orderItem.OrderItemService;
import ru.daniil.order.service.payment.PaymentProcessorServiceImpl;
import ru.daniil.order.service.wallet.WalletService;
import ru.daniil.product.service.product.ProductService;


import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorServiceImplTest {

    @Mock
    private WalletService walletService;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentProcessorServiceImpl paymentProcessorService;

    private User user;
    private User seller;
    private Order order;
    private Wallet wallet;
    private Wallet sellerWallet;
    private WalletTransaction freezeTransaction;
    private List<OrderItem> orderItems;
    private UpdateStatusPaymentRequest request;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setLogin("testUser");

        seller = new User();
        seller.setId(2L);
        seller.setLogin("seller");

        order = new Order(user);
        order.setId(1L);
        order.setOrderNumber("ORD-123");
        order.setStatus(OrderStatus.DRAFT);
        order.setTotalPrice(new BigDecimal("500.00"));

        wallet = new Wallet();
        wallet.setId(1L);
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("1000.00"));
        wallet.setFrozenBalance(BigDecimal.ZERO);

        sellerWallet = new Wallet();
        sellerWallet.setId(2L);
        sellerWallet.setUser(seller);
        sellerWallet.setBalance(BigDecimal.ZERO);
        sellerWallet.setFrozenBalance(BigDecimal.ZERO);

        freezeTransaction = WalletTransaction.builder()
                .id(1L)
                .wallet(wallet)
                .amount(new BigDecimal("500.00"))
                .transactionType(TransactionType.FREEZE)
                .status(TransactionStatus.COMPLETED)
                .order(order)
                .build();

        Product product1 = new Product();
        product1.setId(1L);
        product1.setSku("SKU001");
        product1.setSeller(seller);
        product1.setPriceAtTime(new BigDecimal("300.00"));

        Product product2 = new Product();
        product2.setId(2L);
        product2.setSku("SKU002");
        product2.setSeller(seller);
        product2.setPriceAtTime(new BigDecimal("200.00"));

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setProduct(product1);
        item1.setQuantity(1);
        item1.setPriceAtTime(new BigDecimal("300.00"));

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setProduct(product2);
        item2.setQuantity(1);
        item2.setPriceAtTime(new BigDecimal("200.00"));

        orderItems = Arrays.asList(item1, item2);

        request = new UpdateStatusPaymentRequest();
        request.setOrderNumber("ORD-123");
    }

    @Test
    void initiatePayment_Success_ShouldFreezeFundsAndUpdateStatus() {
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(walletService.getOrCreateWallet(user)).thenReturn(wallet);
        when(walletService.hasSufficientBalance(wallet, order.getTotalPrice())).thenReturn(true);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(orderItems);
        doNothing().when(productService).decreaseStockQuantities(anyMap());
        doNothing().when(walletService).freezeFunds(any(Wallet.class), any(BigDecimal.class), any(Order.class), anyString());
        doNothing().when(orderService).updateStatus(order.getId(), OrderStatus.PROCESSING);
        when(paymentMapper.toInitiateResponse(order)).thenReturn(new PaymentInitiateResponse());

        PaymentInitiateResponse response = paymentProcessorService.initiatePayment(user);

        assertNotNull(response);
        verify(productService).decreaseStockQuantities(anyMap());
        verify(walletService).freezeFunds(eq(wallet), eq(order.getTotalPrice()), eq(order), anyString());
        verify(orderService).updateStatus(order.getId(), OrderStatus.PROCESSING);
    }

    @Test
    void initiatePayment_WhenOrderNotInDraft_ShouldThrowException() {
        order.setStatus(OrderStatus.PROCESSING);
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> paymentProcessorService.initiatePayment(user));

        assertEquals("Оплата возможна только для черновых вариантов заказа", exception.getMessage());
        verify(walletService, never()).freezeFunds(any(), any(), any(), any());
    }

    @Test
    void initiatePayment_WhenInsufficientBalance_ShouldThrowException() {
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(walletService.getOrCreateWallet(user)).thenReturn(wallet);
        when(walletService.hasSufficientBalance(wallet, order.getTotalPrice())).thenReturn(false);
        when(walletService.getAvailableBalance(wallet)).thenReturn(new BigDecimal("100.00"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> paymentProcessorService.initiatePayment(user));

        assertTrue(exception.getMessage().contains("Недостаточно средств"));
        verify(productService, never()).decreaseStockQuantities(anyMap());
    }

    @Test
    void initiatePayment_WhenOrderIsEmpty_ShouldThrowException() {
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(walletService.getOrCreateWallet(user)).thenReturn(wallet);
        when(walletService.hasSufficientBalance(wallet, order.getTotalPrice())).thenReturn(true);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(Collections.emptyList());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> paymentProcessorService.initiatePayment(user));

        assertEquals("Нельзя оплатить пустой заказ", exception.getMessage());
    }

    @Test
    void initiatePayment_WhenProductDecreaseFails_ShouldThrowException() {
        when(orderService.getLastOrCreateOrderByUser(user)).thenReturn(order);
        when(walletService.getOrCreateWallet(user)).thenReturn(wallet);
        when(walletService.hasSufficientBalance(wallet, order.getTotalPrice())).thenReturn(true);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(orderItems);
        doThrow(new IllegalArgumentException("Not enough stock")).when(productService).decreaseStockQuantities(anyMap());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> paymentProcessorService.initiatePayment(user));

        assertTrue(exception.getMessage().contains("Некоторые товары недоступны"));
        verify(walletService, never()).freezeFunds(any(), any(), any(), any());
    }

    @Test
    void completePayment_Success_ShouldWithdrawAndDistributeFunds() {
        order.setStatus(OrderStatus.PROCESSING);
        WalletTransaction withdrawalTransaction = WalletTransaction.builder()
                .id(2L)
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .build();

        when(orderService.getByOrderNumber(request.getOrderNumber())).thenReturn(order);
        when(walletService.getActiveFreezeByOrderNumber(order)).thenReturn(Optional.of(freezeTransaction));
        when(walletService.confirmWithdrawal(freezeTransaction.getId())).thenReturn(withdrawalTransaction);
        when(orderItemService.getByOrderNumber(order.getOrderNumber())).thenReturn(orderItems);
        when(walletService.getOrCreateWallet(seller)).thenReturn(sellerWallet);
        doNothing().when(walletService).increaseBalance(eq(sellerWallet.getId()), any(BigDecimal.class));
        when(walletService.createTransaction(any(Wallet.class), any(BigDecimal.class), any(TransactionType.class),
                any(TransactionStatus.class), anyString(), any(), any(Order.class))).thenReturn(new WalletTransaction());
        when(paymentMapper.toCompleteResponse(order, withdrawalTransaction)).thenReturn(new PaymentCompleteResponse());

        PaymentCompleteResponse response = paymentProcessorService.completePayment(request, user);

        assertNotNull(response);
        verify(walletService).confirmWithdrawal(freezeTransaction.getId());
        verify(walletService, atLeastOnce()).increaseBalance(eq(sellerWallet.getId()), any(BigDecimal.class));
        verify(orderService).updateStatus(order.getId(), OrderStatus.PAID);
    }

    @Test
    void completePayment_WhenOrderNotBelongToUser_ShouldThrowSecurityException() {
        User otherUser = new User();
        otherUser.setId(2L);
        order.setUser(otherUser);

        when(orderService.getByOrderNumber(request.getOrderNumber())).thenReturn(order);

        SecurityException exception = assertThrows(SecurityException.class,
                () -> paymentProcessorService.completePayment(request, user));

        assertEquals("Заказ не принадлежит пользователю", exception.getMessage());
    }

    @Test
    void completePayment_WhenOrderNotInProcessing_ShouldThrowException() {
        order.setStatus(OrderStatus.DRAFT);
        when(orderService.getByOrderNumber(request.getOrderNumber())).thenReturn(order);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> paymentProcessorService.completePayment(request, user));

        assertEquals("Заказ не находится в ожидании оплаты", exception.getMessage());
    }

    @Test
    void completePayment_WhenNoActiveFreeze_ShouldThrowException() {
        order.setStatus(OrderStatus.PROCESSING);
        when(orderService.getByOrderNumber(request.getOrderNumber())).thenReturn(order);
        when(walletService.getActiveFreezeByOrderNumber(order)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> paymentProcessorService.completePayment(request, user));

        assertEquals("Активная заморозка для заказа не найдена", exception.getMessage());
    }

    @Test
    void cancelPayment_Success_ShouldUnfreezeAndRestoreStock() {
        order.setStatus(OrderStatus.PROCESSING);

        when(orderService.getByOrderNumber(request.getOrderNumber())).thenReturn(order);
        when(orderItemService.getByOrderNumber(request.getOrderNumber())).thenReturn(orderItems);
        doNothing().when(productService).increaseStockQuantities(anyMap());
        when(walletService.getActiveFreezeByOrderNumber(order)).thenReturn(Optional.of(freezeTransaction));
        doNothing().when(walletService).unfreezeFunds(freezeTransaction.getId());
        doNothing().when(orderService).updateStatus(order.getId(), OrderStatus.DRAFT);
        when(paymentMapper.toCancelResponse(order.getOrderNumber())).thenReturn(new PaymentCompleteResponse());

        PaymentCompleteResponse response = paymentProcessorService.cancelPayment(request, user);

        assertNotNull(response);
        verify(productService).increaseStockQuantities(anyMap());
        verify(walletService).unfreezeFunds(freezeTransaction.getId());
        verify(orderService).updateStatus(order.getId(), OrderStatus.DRAFT);
    }

    @Test
    void cancelPayment_WhenOrderNotInProcessing_ShouldThrowBadRequestException() {
        order.setStatus(OrderStatus.PAID);
        when(orderService.getByOrderNumber(request.getOrderNumber())).thenReturn(order);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> paymentProcessorService.cancelPayment(request, user));

        assertEquals("Заказ не находится в состоянии ожидания оплаты", exception.getMessage());
    }

    @Test
    void cancelPayment_WhenFreezeTransactionNotFound_ShouldThrowNotFoundException() {
        order.setStatus(OrderStatus.PROCESSING);

        when(orderService.getByOrderNumber(request.getOrderNumber())).thenReturn(order);
        when(orderItemService.getByOrderNumber(request.getOrderNumber())).thenReturn(orderItems);
        doNothing().when(productService).increaseStockQuantities(anyMap());
        when(walletService.getActiveFreezeByOrderNumber(order)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> paymentProcessorService.cancelPayment(request, user));

        assertEquals("Транзакция с замороженными средствами не найдена", exception.getMessage());
    }
}