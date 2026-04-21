package ru.daniil.image.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.daniil.image.service.product.ProductImageService;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/product-images")
@Tag(name = "Api изображений продуктов", description = "API для работы с изображениями продуктов")
public class ProductImageApiController {

    private final ProductImageService productImageService;

    public ProductImageApiController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    @GetMapping("/product/{sku}")
    @Operation(
            summary = "Получение всех изображений продукта",
            description = "Возвращает список полных путей до всех изображений продукта по его артикулу"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список путей к изображениям успешно получен",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = String.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Продукт с указанным артикулом не найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Object> getProductImages(
            @Parameter(description = "Артикул продукта", required = true, example = "SKU-12345")
            @PathVariable String sku) {
        try {
            Map<String, Boolean> imagePaths = productImageService.getProductImages(sku);

            if (imagePaths == null || imagePaths.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyMap());
            }

            return ResponseEntity.ok(imagePaths);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{filename}")
    @Operation(
            summary = "Удаление изображения продукта",
            description = "Удаляет изображение из локального хранилища и БД"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Изображение успешно удалено",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Изображение не найдено",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<String> deleteImage(
            @Parameter(description = "Имя файла изображения", required = true)
            @PathVariable String filename) {
        try {
            productImageService.deleteImage(filename);
            return ResponseEntity.ok("Изображение успешно удалено");
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при удалении: " + e.getMessage());
        }
    }

    @PostMapping("/setMainImage")
    @Operation(
            summary = "Установка главного изображения продукта",
            description = "Устанавливает указанное изображение как главное для продукта. " +
                    "Все остальные изображения этого же продукта автоматически теряют статус главного"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Главное изображение успешно установлено",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Изображение с указанным именем файла не найдено",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Void> setMainImage(
            @Parameter(description = "Имя файла изображения для установки главным")
            @RequestParam String fileName) {

        productImageService.setMain(fileName);
        return ResponseEntity.ok().build();
    }
}
