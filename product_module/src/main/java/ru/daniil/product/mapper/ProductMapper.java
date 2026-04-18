package ru.daniil.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.response.product.ProductResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {
    @Mapping(target = "status",
            expression = "java(product.getStatus() != null ? product.getStatus().name() : null)")
    @Mapping(target = "categoryName", source = "category.name")
    ProductResponse toResponse(Product product);
}