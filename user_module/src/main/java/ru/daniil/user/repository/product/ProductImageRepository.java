package ru.daniil.user.repository.product;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.product.ProductImage;

@Repository
public interface ProductImageRepository extends CrudRepository<ProductImage, Long> {
}
