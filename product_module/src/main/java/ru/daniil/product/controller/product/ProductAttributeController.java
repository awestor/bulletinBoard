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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.product.ProductAttribute;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.sharedInterfaces.UserProvider;
import ru.daniil.product.service.attribute.ProductAttributeService;
import ru.daniil.product.service.product.ProductService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attributes")
@Tag(name = "Api атрибутов продуктов", description = "API для работы с атрибутами продуктов")
public class ProductAttributeController {

    private final UserProvider userProvider;
    private final ProductService productService;
    private final ProductAttributeService productAttributeService;

    public ProductAttributeController(UserProvider userProvider,
                                      ProductService productService,
                                      ProductAttributeService productAttributeService) {
        this.userProvider = userProvider;
        this.productService = productService;
        this.productAttributeService = productAttributeService;
    }


    @PostMapping("/{sku}/attributes")
    @Operation(
            summary = "Добавление атрибута продукту",
            description = "Добавляет новый атрибут к продукту (только свои продукты)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Атрибут успешно добавлен",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Попытка изменить чужой продукт",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Продукт не найден",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> addAttribute(
            @Parameter(description = "sku продукта", required = true)
            @PathVariable String sku,
            @RequestBody Map<String, String> attribute) {

        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = userProvider.getAuthUser();
            Product product = productService.getBySku(sku);

            if (!product.getSeller().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Вы можете изменять только свои товары"));
            }

            String key = attribute.get("key");
            String value = attribute.get("value");

            if (key == null || key.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Ключ атрибута обязателен"));
            }
            if (value == null || value.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Значение атрибута обязательно"));
            }

            ProductAttribute savedAttribute = productAttributeService.save(product, key, value);

            response.put("message", "Атрибут успешно добавлен");
            response.put("attribute", Map.of("id", savedAttribute.getId(), "key", key, "value", value));
            return ResponseEntity.ok(response);

        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Продукт не найден"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Ошибка при добавлении атрибута: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/attributes/{attributeKey}")
    @Operation(
            summary = "Удаление атрибута продукта",
            description = "Удаляет атрибут продукта по ключу (только свои продукты)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Атрибут успешно удалён",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Попытка изменить чужой продукт",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Продукт или атрибут не найдены",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> deleteAttribute(
            @Parameter(description = "ID продукта", required = true)
            @PathVariable Long id,
            @Parameter(description = "Ключ атрибута", required = true)
            @PathVariable String attributeKey) {

        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = userProvider.getAuthUser();
            Product product = productService.getById(id);

            if (!product.getSeller().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Вы можете изменять только свои товары"));
            }

            ProductAttribute attribute = product.getAttributes().stream()
                    .filter(attr -> attr.getKey().equals(attributeKey))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Атрибут не найден"));

            productAttributeService.deleteAttribute(attribute);

            response.put("message", "Атрибут успешно удалён");
            return ResponseEntity.ok(response);

        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Ошибка при удалении атрибута: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/attributes")
    @Operation(
            summary = "Массовое обновление атрибутов продукта",
            description = "Заменяет все атрибуты продукта новыми (только свои продукты)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Атрибуты успешно обновлены",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Попытка изменить чужой продукт",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Продукт не найден",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> updateAttributes(
            @Parameter(description = "ID продукта", required = true)
            @PathVariable Long id,
            @RequestBody Map<String, String> attributes) {

        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = userProvider.getAuthUser();
            Product product = productService.getById(id);

            if (!product.getSeller().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Вы можете изменять только свои товары"));
            }

            Set<ProductAttribute> updatedAttributes = productAttributeService.setMany(product, attributes);

            response.put("message", "Атрибуты успешно обновлены");
            response.put("attributes", updatedAttributes.stream()
                    .collect(Collectors.toMap(ProductAttribute::getKey, ProductAttribute::getValue)));
            return ResponseEntity.ok(response);

        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Продукт не найден"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Ошибка при обновлении атрибутов: " + e.getMessage()));
        }
    }
}
