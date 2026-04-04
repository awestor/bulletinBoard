package ru.daniil.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.daniil.core.entity.base.payment.PaymentMethod;
import ru.daniil.core.request.CreatePaymentMethodRequest;
import ru.daniil.core.response.PaymentMethodResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMethodMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    PaymentMethod toEntity(CreatePaymentMethodRequest request);

    PaymentMethodResponse toResponse(PaymentMethod paymentMethod);

    List<PaymentMethodResponse> toResponseList(List<PaymentMethod> paymentMethods);
}