package ru.daniil.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.daniil.core.entity.base.wallet.Wallet;
import ru.daniil.core.entity.base.wallet.WalletTransaction;
import ru.daniil.core.request.payment.DepositRequest;
import ru.daniil.core.response.wallet.WalletBalanceResponse;
import ru.daniil.core.response.wallet.WalletTransactionResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "walletId", source = "wallet.id")
    @Mapping(target = "availableBalance", source = "availableBalance")
    @Mapping(target = "email", source = "wallet.user.email")
    WalletBalanceResponse toBalanceResponse(Wallet wallet, java.math.BigDecimal availableBalance);

    @Mapping(target = "id", source = "transaction.id")
    @Mapping(target = "amount", source = "transaction.amount")
    @Mapping(target = "transactionType", source = "transaction.transactionType")
    @Mapping(target = "status", source = "transaction.status")
    @Mapping(target = "description", source = "transaction.description")
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "createdAt", source = "transaction.createdAt")
    WalletTransactionResponse toTransactionResponse(WalletTransaction transaction);

    List<WalletTransactionResponse> toTransactionResponseList(List<WalletTransaction> transactions);
}
