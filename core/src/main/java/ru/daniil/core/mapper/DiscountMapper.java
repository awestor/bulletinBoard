package ru.daniil.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.request.DiscountRequest;
import ru.daniil.core.enums.DiscountType;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DiscountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "percentage", ignore = true)
    @Mapping(target = "fixedAmount", ignore = true)
    @Mapping(target = "usageCount", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "type", source = "type", qualifiedByName = "mapStringToType")
    Discount toEntity(DiscountRequest request);

    @Named("mapStringToType")
    default DiscountType mapTypeToString(String type) {
        return type != null ? DiscountType.valueOf(type) : null;
    }
}