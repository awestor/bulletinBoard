package ru.daniil.order.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.wallet.WalletTransaction;
import ru.daniil.core.response.payment.PaymentCompleteResponse;
import ru.daniil.core.response.payment.PaymentInitiateResponse;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class})
public interface PaymentMapper {

    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "totalPrice", source = "order.totalPrice")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "message", constant = "Заказ в ожидании оплаты")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    PaymentInitiateResponse toInitiateResponse(Order order);

    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "transactionId", source = "freezeTransaction.id")
    @Mapping(target = "totalPrice", source = "order.totalPrice")
    @Mapping(target = "status", constant = "COMPLETED")
    @Mapping(target = "message", ignore = true)
    @Mapping(target = "completedAt", expression = "java(LocalDateTime.now())")
    PaymentCompleteResponse toCompleteResponse(Order order, WalletTransaction freezeTransaction);

    @Mapping(target = "orderNumber", source = "orderNumber")
    @Mapping(target = "status", constant = "CANCELLED")
    @Mapping(target = "message", constant = "Оплата отменена, средства разморожены, товары возвращены на склад")
    @Mapping(target = "completedAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    PaymentCompleteResponse toCancelResponse(String orderNumber);
}
