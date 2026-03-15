package ru.daniil.bulletinBoard.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.daniil.bulletinBoard.entity.base.discount.OrderDiscount;
import ru.daniil.bulletinBoard.entity.response.OrderDiscountResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {DiscountMapper.class})
public interface OrderDiscountMapper {

    @Mapping(target = "discountCode", source = "discount.code")
    @Mapping(target = "discountName", source = "discount.name")
    @Mapping(target = "discountType", source = "discount.type")
    @Mapping(target = "discountAmount", source = "discountAmount")
    @Mapping(target = "reservedUntil", source = "reservedUntil")
    OrderDiscountResponse toResponse(OrderDiscount orderDiscount);

    List<OrderDiscountResponse> toResponseList(List<OrderDiscount> orderDiscounts);
}
