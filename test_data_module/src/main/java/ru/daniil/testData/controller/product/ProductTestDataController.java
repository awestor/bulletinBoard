package ru.daniil.testData.controller.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.daniil.product.service.product.ProductService;
import ru.daniil.testData.testDbInit.ProductTestDataGenerator;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/product")
@Tag(name = "Управление тестовыми продуктами",
        description = "Endpoints для генерации и управления тестовыми данными продуктов")
public class ProductTestDataController {

    private final ProductTestDataGenerator productTestDataGenerator;
    private final ProductService productService;

    public ProductTestDataController(ProductTestDataGenerator productTestDataGenerator,
                                     ProductService productService) {
        this.productTestDataGenerator = productTestDataGenerator;
        this.productService = productService;
    }

    @PostMapping("/generate")
    @Operation(
            summary = "Генерация тестовых продуктов",
            description = "Создаёт тестовые продукты для каждой конечной категории (LEAF). " +
                    "Для каждой категории генерируется от 5 до 15 продуктов с разными характеристиками."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Продукты успешно созданы или уже существуют",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера при создании продуктов",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> generateTestProducts() {
        Map<String, Object> response = new HashMap<>();

        long existingCount = productService.count();
        if (existingCount > 50) {
            response.put("message", "Продукты уже существуют в БД, новые не будут созданы");
            response.put("existingProductsCount", existingCount);
            return ResponseEntity.ok(response);
        }

        productTestDataGenerator.generateProducts();

        response.put("message", "Тестовые продукты успешно созданы");
        response.put("totalProducts", productService.count());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    @Operation(
            summary = "Очистка всех продуктов",
            description = "Удаляет все продукты из базы данных. Полезно для перегенерации тестовых данных."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Все продукты успешно удалены",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Ошибка при удалении продуктов",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> clearProducts() {
        productTestDataGenerator.clearProducts();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Все продукты удалены");
        response.put("remainingProducts", productService.count());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Получение статистики продуктов",
            description = "Возвращает детальную статистику по созданным продуктам: общее количество, " +
                    "распределение по категориям, ценовой диапазон. Используется для проверки готовности данных к API тестам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Статистика успешно получена",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> getProductsStats() {
        Map<String, Object> stats = new HashMap<>();

        long total = productService.count();

        stats.put("total", total);

        if (total > 0) {
            stats.put("message", "Данные для API тестов готовы");
            stats.put("hasData", true);
        } else {
            stats.put("message", "Продукты не созданы. Выполните POST /api/test/product/generate");
            stats.put("hasData", false);
        }

        return ResponseEntity.ok(stats);
    }
}