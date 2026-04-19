package ru.daniil.order.orderService;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.entity.base.wallet.Wallet;
import ru.daniil.core.entity.base.wallet.WalletTransaction;
import ru.daniil.core.enums.wallet.TransactionStatus;
import ru.daniil.core.enums.wallet.TransactionType;
import ru.daniil.order.repository.wallet.WalletRepository;
import ru.daniil.order.repository.wallet.WalletTransactionRepository;
import ru.daniil.order.service.wallet.WalletServiceImpl;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private User user;
    private Wallet wallet;
    private Order order;
    private WalletTransaction freezeTransaction;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setLogin("testUser");

        wallet = new Wallet();
        wallet.setId(1L);
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("1000.00"));
        wallet.setFrozenBalance(new BigDecimal("0.00"));

        order = new Order(user);
        order.setId(1L);
        order.setOrderNumber("ORD-123");

        freezeTransaction = WalletTransaction.builder()
                .id(1L)
                .wallet(wallet)
                .amount(new BigDecimal("500.00"))
                .transactionType(TransactionType.FREEZE)
                .status(TransactionStatus.COMPLETED)
                .order(order)
                .build();
    }

    @Test
    void getOrCreateWallet_WhenWalletExists_ShouldReturnExisting() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getOrCreateWallet(user);

        assertEquals(wallet, result);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void getOrCreateWallet_WhenWalletNotExists_ShouldCreateNew() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        Wallet result = walletService.getOrCreateWallet(user);

        assertNotNull(result);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void hasSufficientBalance_WhenBalanceSufficient_ShouldReturnTrue() {
        boolean result = walletService.hasSufficientBalance(wallet, new BigDecimal("500.00"));
        assertTrue(result);
    }

    @Test
    void hasSufficientBalance_WhenBalanceInsufficient_ShouldReturnFalse() {
        boolean result = walletService.hasSufficientBalance(wallet, new BigDecimal("1500.00"));
        assertFalse(result);
    }

    @Test
    void getAvailableBalance_ShouldReturnBalanceMinusFrozen() {
        wallet.setFrozenBalance(new BigDecimal("200.00"));
        BigDecimal result = walletService.getAvailableBalance(wallet);
        assertEquals(new BigDecimal("800.00"), result);
    }

    @Test
    void freezeFunds_Success_ShouldIncreaseFrozenBalanceAndCreateTransaction() {
        when(walletRepository.increaseFrozenBalance(eq(1L), eq(new BigDecimal("500.00")))).thenReturn(1);
        when(transactionRepository.save(any(WalletTransaction.class))).thenReturn(freezeTransaction);

        walletService.freezeFunds(wallet, new BigDecimal("500.00"), order, "Test freeze");

        verify(walletRepository).increaseFrozenBalance(1L, new BigDecimal("500.00"));
        verify(transactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void freezeFunds_WhenInsufficientAvailableBalance_ShouldThrowException() {
        wallet.setBalance(new BigDecimal("500.00"));
        wallet.setFrozenBalance(new BigDecimal("400.00")); // available = 100

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> walletService.freezeFunds(wallet, new BigDecimal("500.00"), order, "Test"));

        assertTrue(exception.getMessage().contains("Недостаточно средств"));
        verify(walletRepository, never()).increaseFrozenBalance(anyLong(), any());
    }

    @Test
    void confirmWithdrawal_Success_ShouldDecreaseBalanceAndFrozenAndCreateWithdrawal() {
        WalletTransaction withdrawalTransaction = WalletTransaction.builder()
                .id(2L)
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(freezeTransaction));
        when(walletRepository.decreaseBalance(1L, new BigDecimal("500.00"))).thenReturn(1);
        when(walletRepository.decreaseFrozenBalance(1L, new BigDecimal("500.00"))).thenReturn(1);
        when(transactionRepository.save(any(WalletTransaction.class))).thenReturn(withdrawalTransaction);

        WalletTransaction result = walletService.confirmWithdrawal(1L);

        assertNotNull(result);
        verify(walletRepository).decreaseBalance(1L, new BigDecimal("500.00"));
        verify(walletRepository).decreaseFrozenBalance(1L, new BigDecimal("500.00"));
    }

    @Test
    void confirmWithdrawal_WhenTransactionNotFound_ShouldThrowException() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> walletService.confirmWithdrawal(999L));

        assertTrue(exception.getMessage().contains("не найдена"));
    }

    @Test
    void confirmWithdrawal_WhenTransactionNotFreeze_ShouldThrowException() {
        WalletTransaction withdrawal = WalletTransaction.builder()
                .transactionType(TransactionType.WITHDRAWAL)
                .build();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(withdrawal));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> walletService.confirmWithdrawal(1L));

        assertEquals("Транзакция не является операцией заморозки", exception.getMessage());
    }

    @Test
    void unfreezeFunds_Success_ShouldDecreaseFrozenAndCreateUnfreezeTransaction() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(freezeTransaction));
        when(walletRepository.decreaseFrozenBalance(1L, new BigDecimal("500.00"))).thenReturn(1);
        when(transactionRepository.save(any(WalletTransaction.class))).thenReturn(new WalletTransaction());

        walletService.unfreezeFunds(1L);

        verify(walletRepository).decreaseFrozenBalance(1L, new BigDecimal("500.00"));
        verify(transactionRepository, times(2)).save(any(WalletTransaction.class)); // unfreeze + freeze status update
    }

    @Test
    void deposit_Success_ShouldIncreaseBalanceAndCreateTransaction() {
        when(walletRepository.increaseBalance(1L, new BigDecimal("200.00"))).thenReturn(1);
        when(transactionRepository.save(any(WalletTransaction.class))).thenReturn(new WalletTransaction());

        WalletTransaction result = walletService.deposit(wallet, new BigDecimal("200.00"), "EXT-123", "Test deposit");

        assertNotNull(result);
        verify(walletRepository).increaseBalance(1L, new BigDecimal("200.00"));
    }

    @Test
    void increaseBalance_WhenWalletNotFound_ShouldThrowException() {
        when(walletRepository.increaseBalance(999L, new BigDecimal("100.00"))).thenReturn(0);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> walletService.increaseBalance(999L, new BigDecimal("100.00")));

        assertTrue(exception.getMessage().contains("не найден"));
    }
}