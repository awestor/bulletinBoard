package ru.daniil.order.repository.wallet;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.wallet.WalletTransaction;
import ru.daniil.core.enums.wallet.TransactionStatus;
import ru.daniil.core.enums.wallet.TransactionType;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends CrudRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWalletId(Long walletId);

    Optional<WalletTransaction> findByOrderAndTransactionTypeAndStatus(
            Order order,
            TransactionType transactionType,
            TransactionStatus status
    );
}