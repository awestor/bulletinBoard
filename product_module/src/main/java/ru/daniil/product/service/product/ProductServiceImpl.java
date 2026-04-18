package ru.daniil.product.service.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.response.product.ProductFilterRequest;
import ru.daniil.product.repository.ProductRepository;
import ru.daniil.product.service.attribute.ProductAttributeService;

import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductAttributeService attributeService;

    public ProductServiceImpl(ProductRepository productRepository,
                              ProductAttributeService attributeService) {
        this.productRepository = productRepository;
        this.attributeService = attributeService;
    }

    @Override
    public Product save(Product product){
        return productRepository.save(product);
    }

    @Override
    public String generateSku() {
        return UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    @Override
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Продукт не найден"));
    }

    @Override
    public Product getBySku(String sku) {
        return productRepository.findBySku(sku);
    }

    @Override
    public List<Product> getByCategory(String categoryName) {
        return productRepository.findByCategoryName(categoryName);
    }

    @Override
    public Page<Product> getBySeller(Long userId, Pageable pageable) {
        return productRepository.findBySellerId(userId, pageable);
    }

    @Override
    public Page<Product> filterProducts(ProductFilterRequest filter, Pageable pageable) {
        filter.normalizePriceRange();

        if (!filter.hasAnyFilter()) {
            return productRepository.findAll(pageable);
        }

        // Использование универсального метода
        Specification<Product> spec = ProductRepository.withFilters(
                filter.getInStock(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                filter.getNamePart(),
                filter.getSkuPart(),
                filter.getCategoryName(),
                filter.getSellerLogin()
        );

        return productRepository.findAll(spec, pageable);
    }

    @Override
    public long count() {
        return productRepository.count();
    }

    @Transactional
    @Override
    public void updateStock(Long id, Integer quantity) {
        Product product = getById(id);
        product.setStockQuantity(quantity);
        productRepository.save(product);
    }

    @Transactional
    @Override
    public void updateCategoryForProducts(List<Product> products, Category newCategory){
        productRepository.updateCategoryForProducts(products, newCategory);
    }

    @Transactional
    @Override
    public void delete(Product product) {
        productRepository.delete(product);
    }

    @Transactional
    @Override
    public void deleteAll() {
        productRepository.deleteAllInBatch();
    }
}
