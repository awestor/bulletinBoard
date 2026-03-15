package ru.daniil.bulletinBoard.mapper;

import org.mapstruct.*;
import ru.daniil.bulletinBoard.entity.base.payment.PaymentInfo;
import ru.daniil.bulletinBoard.entity.request.CreatePaymentInfoRequest;
import ru.daniil.bulletinBoard.entity.response.PaymentInfoResponse;
import ru.daniil.bulletinBoard.enums.PaymentStatus;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {PaymentMethodMapper.class})
public interface PaymentInfoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "method", ignore = true)
    @Mapping(target = "status", expression = "java(ru.daniil.bulletinBoard.enums.PaymentStatus.PROCESSING)")
    @Mapping(target = "createdAt", ignore = true)
    PaymentInfo toEntity(CreatePaymentInfoRequest request);

    @Mapping(target = "paymentMethodType", source = "method.type")
    @Mapping(target = "paidCost", source = "totalPrice")
    @Mapping(target = "status", source = "status", qualifiedByName = "mapStatusToString")
    PaymentInfoResponse toResponse(PaymentInfo paymentInfo);

    @Named("mapStatusToString")
    default String mapStatusToString(PaymentStatus status) {
        return status != null ? status.toString() : null;
    }
}