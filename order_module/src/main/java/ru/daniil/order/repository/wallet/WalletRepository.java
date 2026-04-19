package ru.daniil.order.repository.wallet;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.wallet.Wallet;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WalletRepository extends CrudRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount WHERE w.id = :walletId")
    int increaseBalance(@Param("walletId") Long walletId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance - :amount WHERE w.id = :walletId AND w.balance >= :amount")
    int decreaseBalance(@Param("walletId") Long walletId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Wallet w SET w.frozenBalance = w.frozenBalance + :amount WHERE w.id = :walletId")
    int increaseFrozenBalance(@Param("walletId") Long walletId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Wallet w SET w.frozenBalance = w.frozenBalance - :amount WHERE w.id = :walletId AND w.frozenBalance >= :amount")
    int decreaseFrozenBalance(@Param("walletId") Long walletId, @Param("amount") BigDecimal amount);
}
