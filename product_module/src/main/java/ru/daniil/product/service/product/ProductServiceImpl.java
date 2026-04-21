package ru.daniil.product.service.product;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.response.product.ProductFilterRequest;
import ru.daniil.product.repository.ProductRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
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

    @Override
    @Transactional
    public void decreaseStockQuantities(Map<String, Integer> skuQuantityMap) {
        List<String> skus = List.copyOf(skuQuantityMap.keySet());
        List<Product> products = productRepository.findBySkuIn(skus);

        if (products.size() != skus.size()) {
            List<String> foundSkus = products.stream()
                    .map(Product::getSku)
                    .toList();
            List<String> missingSkus = skus.stream()
                    .filter(sku -> !foundSkus.contains(sku))
                    .toList();
            throw new EntityNotFoundException("Товары не найдены: " + missingSkus);
        }

        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getSku, p -> p));

        for (Map.Entry<String, Integer> entry : skuQuantityMap.entrySet()) {
            String sku = entry.getKey();
            Integer requestedQuantity = entry.getValue();
            Product product = productMap.get(sku);

            if (product.getStockQuantity() < requestedQuantity) {
                throw new IllegalArgumentException(
                        String.format("Недостаточно товара %s на складе. Доступно: %d, требуется: %d",
                                sku, product.getStockQuantity(), requestedQuantity)
                );
            }
        }

        for (Map.Entry<String, Integer> entry : skuQuantityMap.entrySet()) {
            String sku = entry.getKey();
            Integer quantity = entry.getValue();
            Product product = productMap.get(sku);
            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);
        }
    }

    @Override
    @Transactional
    public void increaseStockQuantities(Map<String, Integer> skuQuantityMap) {
        List<String> skus = List.copyOf(skuQuantityMap.keySet());
        List<Product> products = productRepository.findBySkuIn(skus);

        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getSku, p -> p));

        for (Map.Entry<String, Integer> entry : skuQuantityMap.entrySet()) {
            String sku = entry.getKey();
            Integer quantity = entry.getValue();
            Product product = productMap.get(sku);
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + quantity);
                productRepository.save(product);
            }
        }
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
