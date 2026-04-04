package ru.daniil.core.mapper;

import org.mapstruct.*;
import ru.daniil.core.entity.base.product.ProductImage;
import ru.daniil.core.response.ProductImageResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductImageMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "fullPath", source = "path", qualifiedByName = "buildFullImagePath")
    @Mapping(target = "isMain", source = "isMain")
    ProductImageResponse toResponse(ProductImage productImage, @Context String productImagePath);

    List<ProductImageResponse> toResponseList(List<ProductImage> productImages, @Context String productImagePath);

    @Named("buildFullImagePath")
    default String buildFullImagePath(String path, @Context String productImagePath) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String basePath = productImagePath.endsWith("/") ? productImagePath : productImagePath + "/";
        String imagePath = path.startsWith("/") ? path.substring(1) : path;
        return basePath + imagePath;
    }
}