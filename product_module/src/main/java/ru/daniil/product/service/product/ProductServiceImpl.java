package ru.daniil.product.service.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.product.ProductAttribute;
import ru.daniil.core.entity.base.product.ProductImage;
import ru.daniil.core.request.CreateProductRequest;
import ru.daniil.product.repository.ProductRepository;
import ru.daniil.product.service.attribute.ProductAttributeService;
import ru.daniil.product.service.image.ProductImageService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductImageService productImageService;
    private final ProductAttributeService attributeService;

    public ProductServiceImpl(ProductRepository productRepository,
                              ProductImageService productImageService,
                              ProductAttributeService attributeService) {
        this.productRepository = productRepository;
        this.productImageService = productImageService;
        this.attributeService = attributeService;
    }

    @Transactional
    @Override
    public Product create(CreateProductRequest request, Category category) {
        if (!category.isLeaf()) {
            throw new RuntimeException("Продукт не может быть размещён в не конечной категории");
        }

        Product product = new Product(
                category,
                request.getName(),
                request.getPrice()
        );

        product.setDescription(request.getDescription());
        product.setSku(generateSku());
        product.setStockQuantity(request.getStockQuantity());

        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            product.setAttributes(attributeService.saveMany(product, request.getAttributes()));
        }

        if (request.getImages() != null) {
            request.getImages().forEach(image ->
                {
                    try {
                        ProductImage productImage = new ProductImage(
                                product, productImageService.saveImage(image), false);
                        productImageService.save(productImage);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
        }

        return productRepository.save(product);
    }

    private String generateSku() {
        return UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }


    @Transactional
    @Override
    public Product update(Long id, CreateProductRequest request, Category newCategory) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Продукт не найден"));

        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategory().getId())) {

            if (!newCategory.isLeaf()) {
                throw new RuntimeException("Продукт не может быть назначен в не конечную категорию");
            }

            product.setCategory(newCategory);
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());

        product.setStockQuantity(request.getStockQuantity());

        if (request.getAttributes() != null) {
            product.setAttributes(attributeService.setMany(product, request.getAttributes()));
        }

        return productRepository.save(product);
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
    public void delete(Long id) {
        Product product = getById(id);
        for (ProductAttribute attribute : product.getAttributes()) {
            attributeService.deleteAttribute(attribute);
        }
        productRepository.delete(product);
    }
}
