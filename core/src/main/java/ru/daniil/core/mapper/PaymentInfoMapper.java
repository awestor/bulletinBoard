package ru.daniil.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import ru.daniil.core.entity.base.payment.PaymentInfo;
import ru.daniil.core.request.CreatePaymentInfoRequest;
import ru.daniil.core.response.PaymentInfoResponse;
import ru.daniil.core.enums.PaymentStatus;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {PaymentMethodMapper.class})
public interface PaymentInfoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "method", ignore = true)
    @Mapping(target = "status", expression = "java(ru.daniil.core.enums.PaymentStatus.PROCESSING)")
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