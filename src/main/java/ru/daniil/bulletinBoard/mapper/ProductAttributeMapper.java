package ru.daniil.bulletinBoard.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.daniil.bulletinBoard.entity.base.product.ProductAttribute;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductAttributeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    ProductAttribute toEntity(String key, String value);
}