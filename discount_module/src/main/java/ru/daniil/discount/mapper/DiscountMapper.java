package ru.daniil.discount.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.enums.DiscountType;
import ru.daniil.core.request.discount.CreateDiscountRequest;
import ru.daniil.core.response.discount.DiscountInfo;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DiscountMapper {

    @Mapping(target = "discountCode", source = "code")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "percentage", source = "percentage")
    @Mapping(target = "fixedAmount", source = "fixedAmount")
    @Mapping(target = "applicableCategoryId", source = "applicableCategoryId")
    DiscountInfo toDiscountInfo(Discount discount);

    List<DiscountInfo> toDiscountInfoList(List<Discount> discounts);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "type", expression = "java(ru.daniil.core.enums.DiscountType.valueOf(request.getType()))")
    @Mapping(target = "percentage", source = "valuePercentage")
    @Mapping(target = "fixedAmount", source = "valueAmount")
    @Mapping(target = "applicableCategoryId", expression = "java(request.getApplicableCategoryId() != null ? request.getApplicableCategoryId() : -1L)")
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    @Mapping(target = "usageLimit", source = "usageLimit")
    @Mapping(target = "usageCount", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    Discount toDiscount(CreateDiscountRequest request);
}