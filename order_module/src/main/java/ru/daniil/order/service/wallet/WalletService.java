package ru.daniil.order.service.wallet;

import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.entity.base.wallet.Wallet;
import ru.daniil.core.entity.base.wallet.WalletTransaction;
import ru.daniil.core.enums.wallet.TransactionStatus;
import ru.daniil.core.enums.wallet.TransactionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WalletService {
    /**
     * Метод получения или создания кошелька для пользователя
     * @param user пользователь для которого запрашивается кошелёк
     * @return сущность кошелька
     */
    Wallet getOrCreateWallet(User user);

    /**
     * Метод проверки достаточности денег в кошельке пользователя для успешности транзакции
     * @param wallet сущность кошелька
     * @param amount требуемая сумма
     * @return true или false
     */
    boolean hasSufficientBalance(Wallet wallet, BigDecimal amount);

    /**
     * Метод для получения доступного баланса
     * @param wallet  сущность кошелька
     * @return имеющееся средства
     */
    BigDecimal getAvailableBalance(Wallet wallet);

    /**
     * Метод заморозки денег на счету пользователя
     * @param wallet кошелёк
     * @param amount сумма заморозки
     * @param order заказ по которому выполняется заморозка
     * @param description описание
     */
    void freezeFunds(Wallet wallet, BigDecimal amount,
                     Order order, String description);

    /**
     * Метод отвечающий за списание замороженных средств с кошелька пользователя
     * @param transactionId идентификатор транзакции
     * @return полученная после обновления транзакция
     */
    WalletTransaction confirmWithdrawal(Long transactionId);

    /**
     * Метод отвечающий за обработку ошибки или отмены оплаты заказа,
     * что разморозит средства на кошельке пользователя
     * @param transactionId идентификатор транзакции
     */
    void unfreezeFunds(Long transactionId);

    /**
     * Метод для пополнения кошелька пользователя
     * @param wallet сущность кошелька
     * @param amount пополняемое количество
     * @param externalDepositId абстрактный код, что получается когда ты производишь оплату картой
     *                         (произвольная строка подходящая под регекс)
     * @param description описание
     * @return сохранённая транзакция
     */
    WalletTransaction deposit(Wallet wallet, BigDecimal amount,
                              String externalDepositId, String description);

    /**
     * Метод получения транзакции по id
     * @param id идентификатор транзакции
     * @return найденная транзакция или EntityNotFound
     */
    WalletTransaction getTransactionById(Long id);

    /**
     * Метод для получения всех транзакций произведённых по одному кошельку
     * @param wallet сущность кошелька
     * @return список транзакций
     */
    List<WalletTransaction> getTransactionsByWallet(Wallet wallet);

    /**
     * Метод для получения транзакции заморозки, что ожидает оплаты
     * @param order сущность заказа к которой прикреплена транзакция
     * @return найденная транзакция или ничего
     */
    Optional<WalletTransaction> getActiveFreezeByOrderNumber(Order order);

    /**
     * Метод для создания транзакции
     * @param wallet сущность кошелька
     * @param amount требуемое количество
     * @param type тип операции
     * @param status статус операции
     * @param description описание
     * @param externalDepositId произвольная строка подходящая под регекс
     * @param order сущность заказа
     * @return созданная транзакция
     */
    WalletTransaction createTransaction(Wallet wallet, BigDecimal amount,
                                        TransactionType type, TransactionStatus status,
                                        String description, String externalDepositId, Order order);

    void increaseBalance(Long walletId, BigDecimal amount);

    void decreaseBalance(Long walletId, BigDecimal amount);

    void increaseFrozenBalance(Long walletId, BigDecimal amount);

    void decreaseFrozenBalance(Long walletId, BigDecimal amount);
}
