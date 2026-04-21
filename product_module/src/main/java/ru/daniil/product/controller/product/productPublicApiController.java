package ru.daniil.product.controller.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.response.product.ProductFilterRequest;
import ru.daniil.core.response.product.ProductResponse;
import ru.daniil.product.mapper.ProductMapper;
import ru.daniil.product.service.product.ProductService;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/products")
@Tag(name = "Публичные Api продуктов", description = "API для работы с продуктами, что не требуют авторизацию")
public class productPublicApiController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public productPublicApiController(ProductService productService,
                                ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @GetMapping("/sku/{sku}")
    @Operation(
            summary = "Получение продукта по SKU",
            description = "Возвращает продукт по его уникальному артикулу (SKU)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Продукт успешно найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Продукт с указанным SKU не найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ProductResponse> getProductBySku(
            @Parameter(description = "Артикул продукта (SKU)", required = true)
            @PathVariable String sku) {

        Product product = productService.getBySku(sku);
        return ResponseEntity.ok(productMapper.toResponse(product));
    }

    @PostMapping("/filter")
    @Operation(
            summary = "Фильтрация продуктов",
            description = """
                    Возвращает список продуктов с применением фильтров.
                    
                    **Доступные фильтры:**
                    - `inStock` (boolean) - только товары в наличии (stockQuantity > 0)
                    - `minPrice` (BigDecimal) - минимальная цена (≥ 0)
                    - `maxPrice` (BigDecimal) - максимальная цена (≥ 0)
                    - `namePart` (string) - часть названия продукта
                    - `skuPart` (string) - часть SKU продукта
                    - `categoryName` (string) - точное название категории
                    - `sellerLogin` (string) - точный логин продавца
                    
                    **Валидация:**
                    - `minPrice` не может быть больше `maxPrice`
                    - Все поля, кроме первых 3-х, опциональны
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список продуктов успешно получен",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные параметры фильтрации (например, minPrice > maxPrice)",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> filterProducts(
            @Valid @RequestBody ProductFilterRequest filter,

            @Parameter(description = "Номер страницы (начиная с 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Количество элементов на странице")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Поле для сортировки (id, name, price, createdAt, stockQuantity)")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Направление сортировки (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Product> products = productService.filterProducts(filter, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("products", products.getContent().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList()));
        response.put("currentPage", products.getNumber());
        response.put("totalItems", products.getTotalElements());
        response.put("totalPages", products.getTotalPages());
        response.put("filtersApplied", filter);

        return ResponseEntity.ok(response);
    }
}
