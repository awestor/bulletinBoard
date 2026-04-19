package ru.daniil.order.service.wallet;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.entity.base.wallet.Wallet;
import ru.daniil.core.entity.base.wallet.WalletTransaction;
import ru.daniil.core.enums.wallet.TransactionStatus;
import ru.daniil.core.enums.wallet.TransactionType;
import ru.daniil.order.repository.wallet.WalletRepository;
import ru.daniil.order.repository.wallet.WalletTransactionRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository,
                             WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = walletTransactionRepository;
    }

    @Override
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUser(user);
                    wallet.setBalance(BigDecimal.ZERO);
                    wallet.setFrozenBalance(BigDecimal.ZERO);
                    return walletRepository.save(wallet);
                });
    }

    @Override
    @Transactional
    public WalletTransaction deposit(Wallet wallet, BigDecimal amount, String externalDepositId, String description) {
        WalletTransaction depositTransaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .externalDeposit(externalDepositId)
                .build();

        increaseBalance(wallet.getId(), amount);

        return transactionRepository.save(depositTransaction);
    }

    @Override
    public boolean hasSufficientBalance(Wallet wallet, BigDecimal amount) {
        return getAvailableBalance(wallet).compareTo(amount) >= 0;
    }

    @Override
    public BigDecimal getAvailableBalance(Wallet wallet) {
        return wallet.getBalance().subtract(wallet.getFrozenBalance());
    }

    @Override
    @Transactional
    public void freezeFunds(Wallet wallet, BigDecimal amount, Order order, String description) {
        BigDecimal availableBalance = getAvailableBalance(wallet);

        if (availableBalance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                    String.format("Недостаточно средств. Доступно: %s, требуется: %s", availableBalance, amount)
            );
        }

        increaseFrozenBalance(wallet.getId(), amount);

        createTransaction(
                wallet, amount, TransactionType.FREEZE, TransactionStatus.COMPLETED,
                description, null, order
        );
    }

    @Override
    @Transactional
    public WalletTransaction confirmWithdrawal(Long transactionId) {
        WalletTransaction freezeTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Транзакция не найдена: " + transactionId));

        if (freezeTransaction.getTransactionType() != TransactionType.FREEZE) {
            throw new IllegalStateException("Транзакция не является операцией заморозки");
        }

        if (freezeTransaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalStateException("Транзакция заморозки не завершена");
        }

        Wallet wallet = freezeTransaction.getWallet();
        BigDecimal amount = freezeTransaction.getAmount();

        decreaseBalance(wallet.getId(), amount);
        decreaseFrozenBalance(wallet.getId(), amount);

        return createTransaction(
                wallet, amount, TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED,
                "Списание по заказу: " + freezeTransaction.getOrder().getOrderNumber(),
                null, freezeTransaction.getOrder()
        );
    }

    @Override
    @Transactional
    public void unfreezeFunds(Long transactionId) {
        WalletTransaction freezeTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Транзакция не найдена: " + transactionId));

        if (freezeTransaction.getTransactionType() != TransactionType.FREEZE) {
            throw new IllegalStateException("Транзакция не является операцией заморозки");
        }

        if (freezeTransaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalStateException("Транзакция заморозки не завершена");
        }

        Wallet wallet = freezeTransaction.getWallet();
        BigDecimal amount = freezeTransaction.getAmount();

        decreaseFrozenBalance(wallet.getId(), amount);

        createTransaction(
                wallet, amount, TransactionType.UNFREEZE, TransactionStatus.COMPLETED,
                "Разморозка по заказу: " + freezeTransaction.getOrder().getOrderNumber(),
                null, freezeTransaction.getOrder()
        );

        freezeTransaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(freezeTransaction);
    }

    @Override
    @Transactional
    public void increaseBalance(Long walletId, BigDecimal amount) {
        int updatedRows = walletRepository.increaseBalance(walletId, amount);
        if (updatedRows == 0) {
            throw new EntityNotFoundException("Кошелек не найден: " + walletId);
        }
    }

    @Override
    @Transactional
    public void decreaseBalance(Long walletId, BigDecimal amount) {
        int updatedRows = walletRepository.decreaseBalance(walletId, amount);
        if (updatedRows == 0) {
            throw new EntityNotFoundException("Кошелек не найден или недостаточно средств: " + walletId);
        }
    }

    @Override
    @Transactional
    public void increaseFrozenBalance(Long walletId, BigDecimal amount) {
        int updatedRows = walletRepository.increaseFrozenBalance(walletId, amount);
        if (updatedRows == 0) {
            throw new EntityNotFoundException("Кошелек не найден: " + walletId);
        }
    }

    @Override
    @Transactional
    public void decreaseFrozenBalance(Long walletId, BigDecimal amount) {
        int updatedRows = walletRepository.decreaseFrozenBalance(walletId, amount);
        if (updatedRows == 0) {
            throw new EntityNotFoundException("Кошелек не найден: " + walletId);
        }
    }

    @Override
    public WalletTransaction createTransaction(Wallet wallet, BigDecimal amount,
                                               TransactionType type, TransactionStatus status,
                                               String description, String externalDepositId, Order order) {
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(type)
                .status(status)
                .description(description)
                .externalDeposit(externalDepositId)
                .order(order)
                .build();

        return transactionRepository.save(transaction);
    }

    @Override
    public WalletTransaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Транзакция не найдена: " + id));
    }

    @Override
    public List<WalletTransaction> getTransactionsByWallet(Wallet wallet) {
        return transactionRepository.findByWalletId(wallet.getId());
    }

    @Override
    public Optional<WalletTransaction> getActiveFreezeByOrderNumber(Order order) {
        return transactionRepository.findByOrderAndTransactionTypeAndStatus(
                order, TransactionType.FREEZE, TransactionStatus.COMPLETED
        );
    }
}