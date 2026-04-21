package ru.daniil.product.service.product;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.product.ProductAttribute;
import ru.daniil.core.entity.base.product.ProductImage;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.exceptions.UserBlockedException;
import ru.daniil.core.request.CreateUpdateProductRequest;
import ru.daniil.image.service.product.ProductImageService;
import ru.daniil.product.service.attribute.ProductAttributeService;
import ru.daniil.product.service.category.CategoryService;

import java.util.ArrayList;
import java.util.List;

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
    public Product create(CreateUpdateProductRequest request, User user) {
        if (user.isTradingBlocked()) {
            throw new UserBlockedException("Пользователю запрещено выставлять объявления о продаже");
        }

        Category category =  categoryService.getByName(request.getCategoryName());
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
    private String addProductImage(MultipartFile file, String sku) {
        Product product = productService.getBySku(sku);
        String productName = productImageService.saveImage(file);
        ProductImage productImage = ProductImage.builder()
                .product(product)
                .name(productName)
                .isMain(false)
                .build();

        productImageService.save(productImage);
        return productImageService.completePath(productName);
    }

    @Override
    public List<String> addManyProductImages(String sku, List<MultipartFile> files) {
        try{
            List<String> results = new ArrayList<>();
            for (MultipartFile file :files){
                results.add(addProductImage(
                        file,
                        sku
                ));
            }
            return results;
        }
        catch (Exception e){
            throw new RuntimeException("Ошибка при загрузке изображений: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public Product update(Long id, CreateUpdateProductRequest request) {
        Product product;
        try{
            product = productService.getById(id);
            if (!request.getCategoryName().equals(product.getCategory().getName())) {

                Category newCategory = categoryService.getByName(request.getCategoryName());

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
