package ru.daniil.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.request.CreateProductRequest;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CategoryMapper.class, ProductImageMapper.class, ProductAttributeMapper.class})
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "discount", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(CreateProductRequest request);

    @Named("getCategoryPath")
    default String getCategoryPath(Category category) {
        return category != null ? category.getFullPath() : null;
    }

    @Named("getMainImage")
    default String getMainImage(List<ru.daniil.core.entity.base.product.ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsMain()))
                .findFirst()
                .map(ru.daniil.core.entity.base.product.ProductImage::getName)
                .orElse(images.getFirst().getName());
    }
}