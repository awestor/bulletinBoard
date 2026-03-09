package ru.daniil.bulletinBoard.mapper;

import org.mapstruct.*;
import ru.daniil.bulletinBoard.entity.base.product.Category;
import ru.daniil.bulletinBoard.entity.request.CreateCategoryRequest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CreateCategoryRequest request);
}