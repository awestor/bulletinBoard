package ru.daniil.product.controller.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.product.service.product.ProductProcessorService;
import ru.daniil.product.service.product.ProductService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@Tag(name = "Api продуктов для администраторов", description = "API для работы с продуктами для администраторов")
public class ProductAdminApiController {

    private final ProductService productService;
    private final ProductProcessorService productProcessorService;

    public ProductAdminApiController(ProductService productService,
                                ProductProcessorService productProcessorService) {
        this.productService = productService;
        this.productProcessorService = productProcessorService;
    }

    @DeleteMapping("/{sku}")
    @Operation(
            summary = "Удаление продукта",
            description = "Удаляет продукт по его ID (только свои продукты)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Продукт успешно удалён",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Попытка удалить чужой продукт",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Продукт не найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> deleteProduct(
            @Parameter(description = "ID продукта", required = true)
            @PathVariable String sku) {

        Map<String, Object> response = new HashMap<>();
        try {
            Product product = productService.getBySku(sku);

            productProcessorService.delete(product.getId());

            response.put("message", "Продукт успешно удалён");
            return ResponseEntity.ok(response);

        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Продукт не найден"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Ошибка при удалении продукта: " + e.getMessage()));
        }
    }
}
