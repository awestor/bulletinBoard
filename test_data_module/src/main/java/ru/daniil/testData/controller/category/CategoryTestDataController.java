package ru.daniil.testData.controller.category;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.daniil.testData.testDbInit.CategoryTestDataGenerator;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/categpry")
@Tag(name = "Управление тестовыми категориями",
        description = "API для генерации и управления тестовыми данными продуктов")
public class CategoryTestDataController {

    private final CategoryTestDataGenerator categoryTestDataGenerator;

    public CategoryTestDataController(CategoryTestDataGenerator categoryTestDataGenerator) {
        this.categoryTestDataGenerator = categoryTestDataGenerator;
    }

    @PostMapping("/generate")
    @Operation(
            summary = "Генерация тестовых категорий",
            description = "Создаёт иерархию тестовых категорий (корневые, промежуточные и конечные). " +
                    "Если категорий уже больше 10 - генерация пропускается."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Категории успешно сгенерированы или уже существуют",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера при генерации категорий",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> generateTestCategories() {
        Map<String, Object> response = new HashMap<>();
        try {
            categoryTestDataGenerator.generateCategories();
            response.put("success", true);
            response.put("message", "Тестовые категории успешно сгенерированы");
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при генерации категорий: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/clear")
    @Operation(
            summary = "Очистка всех категорий",
            description = "Удаляет все категории из базы данных"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Все категории успешно удалены",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера при удалении категорий",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> clearCategories() {
        Map<String, Object> response = new HashMap<>();
        try {
            categoryTestDataGenerator.clearCategories();
            response.put("success", true);
            response.put("message", "Все категории успешно удалены из базы данных");
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при удалении категорий: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/regenerate")
    @Operation(
            summary = "Перегенерация категорий",
            description = "Очищает существующие категории и создаёт новые"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Категории успешно перегенерированы",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> regenerateCategories() {
        Map<String, Object> response = new HashMap<>();
        try {
            categoryTestDataGenerator.clearCategories();
            categoryTestDataGenerator.generateCategories();
            response.put("success", true);
            response.put("message", "Категории успешно перегенерированы");
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при перегенерации категорий: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/status")
    @Operation(
            summary = "Проверка статуса генератора",
            description = "Возвращает информацию о текущем состоянии генератора тестовых данных"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Статус успешно получен",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", "ready");
        response.put("message", "Генератор тестовых категорий доступен");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("endpoints", Map.of(
                "generate", "POST /api/test/categories/generate - создание тестовых категорий",
                "clear", "DELETE /api/test/categories/clear - удаление всех категорий",
                "regenerate", "POST /api/test/categories/regenerate - перегенерация категорий",
                "status", "GET /api/test/categories/status - проверка статуса"
        ));
        return ResponseEntity.ok(response);
    }
}
