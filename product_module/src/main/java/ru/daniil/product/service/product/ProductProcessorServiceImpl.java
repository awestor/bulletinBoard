package ru.daniil.product.service.product;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.product.ProductAttribute;
import ru.daniil.core.entity.base.product.ProductImage;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.exceptions.UserBlockedExeption;
import ru.daniil.core.request.CreateProductRequest;
import ru.daniil.core.response.product.ProductFilterRequest;
import ru.daniil.image.service.product.ProductImageService;
import ru.daniil.product.service.attribute.ProductAttributeService;
import ru.daniil.product.service.category.CategoryService;

@Service
public class ProductProcessorServiceImpl implements ProductProcessorService {

    private final ProductService productService;
    private final ProductImageService productImageService;
    private final ProductAttributeService attributeService;
    private final CategoryService categoryService;

    private static final Logger infoLogger = LoggerFactory.getLogger("INFO-LOGGER");

    public ProductProcessorServiceImpl(ProductService productService,
                                       ProductImageService productImageService,
                                       ProductAttributeService attributeService,
                                       CategoryService categoryService) {
        this.productService = productService;
        this.productImageService = productImageService;
        this.attributeService = attributeService;
        this.categoryService = categoryService;
    }

    @Transactional
    @Override
    public Product create(CreateProductRequest request, User user) {
        if (user.isTradingBlocked()) {
            throw new UserBlockedExeption("Пользователю запрещено выставлять объявления о продаже");
        }

        Category category =  categoryService.getById(request.getCategoryId());
        if (!category.isLeaf()) {
            throw new BadRequestException("Продукт не может быть размещён в не конечной категории");
        }


        Product product = new Product(
                category,
                user,
                request.getName(),
                request.getPrice()
        );

        product.setDescription(request.getDescription());
        product.setStockQuantity(request.getStockQuantity());

        product.setSku(productService.generateSku());
        try{
            Product savedProduct = productService.save(product);

            if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
                savedProduct.setAttributes(attributeService.saveMany(savedProduct, request.getAttributes()));
            }

            if (request.getImages() != null) {
                for (MultipartFile image : request.getImages()) {
                    String imageName = productImageService.saveImage(image);
                    try {

                        ProductImage productImage = new ProductImage(
                                savedProduct,
                                imageName,
                                false
                        );
                        productImageService.save(productImage);
                    } catch (Exception e) {
                        throw new RuntimeException("Не удалось сохранить изображение", e);
                    }
                }
            }
            return savedProduct;

        } catch (Exception e){
            throw new RuntimeException("Продукт не был создан. Причина: " + e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public Product update(Long id, CreateProductRequest request, Category newCategory) {
        Product product;
        try{
            product = productService.getById(id);
            if (request.getCategoryId() != null && !request.getCategoryId()
                    .equals(product.getCategory().getId())) {

                if (!newCategory.isLeaf()) {
                    throw new BadRequestException("Продукт не может быть назначен в не конечную категорию");
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

            return productService.save(product);
        } catch (BadRequestException e){
            infoLogger.error(e.getMessage());
            throw e;
        } catch (Exception ex){
            infoLogger.error(ex.getMessage());
            throw new NotFoundException("Продукт для редактирования не был найден");
        }
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Product product = productService.getById(id);
        productImageService.deleteAllImages(product.getId());
        for (ProductAttribute attribute : product.getAttributes()) {
            attributeService.deleteAttribute(attribute);
        }
        productService.delete(product);
    }

    @Transactional
    @Override
    public void deleteAll() {
        productImageService.deleteAllinButch();
        productService.deleteAll();
    }
}
