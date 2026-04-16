package ru.daniil.product.controller.category;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.daniil.core.enums.CategoryType;
import ru.daniil.product.config.testBdInit.CategoryTestDataGenerator;
import ru.daniil.product.repository.CategoryRepository;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/category")
@Tag(name = "Управление тестовыми категориями",
        description = "Endpoints для генерации и управления тестовыми данными категорий")
public class CategoryTestDataController {

    private final CategoryTestDataGenerator categoryTestDataGenerator;
    private final CategoryRepository categoryRepository;

    public CategoryTestDataController(CategoryTestDataGenerator categoryTestDataGenerator,
                                      CategoryRepository categoryRepository) {
        this.categoryTestDataGenerator = categoryTestDataGenerator;
        this.categoryRepository = categoryRepository;
    }

    @PostMapping("/generate")
    @Operation(
            summary = "Генерация тестовых категорий",
            description = "Создаёт иерархическую структуру тестовых категорий (по умолчанию 125 категорий всего)" +
                    " для API тестирования. Включает корневые, промежуточные и конечные категории" +
                    " с иерархией ROOT → INTERMEDIATE → LEAF."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Категории успешно созданы или уже существуют",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера при создании категорий",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> generateTestCategories() {
        Map<String, Object> response = new HashMap<>();

        long existingCount = categoryRepository.count();
        if (existingCount > 10) {
            response.put("message", "Категории уже существуют в БД, новые не будут созданы");
            response.put("existingCategoriesCount", existingCount);
            return ResponseEntity.ok(response);
        }

        categoryTestDataGenerator.generateCategories();

        response.put("message", "Тестовые категории успешно созданы");
        response.put("totalCategories", categoryRepository.count());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    @Operation(
            summary = "Очистка всех категорий",
            description = "Удаляет все категории из базы данных. Полезно для перегенерации тестовых данных."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Все категории успешно удалены",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Ошибка при удалении категорий",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> clearCategories() {
        categoryTestDataGenerator.clearCategories();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Все категории удалены");
        response.put("remainingCategories", categoryRepository.count());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Получение статистики категорий",
            description = "Возвращает детальную статистику по созданным категориям: общее количество, " +
                    "количество корневых, промежуточных и конечных категорий. " +
                    "Используется для проверки готовности данных к API тестам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Статистика успешно получена",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> getCategoriesStats() {
        Map<String, Object> stats = new HashMap<>();

        long total = categoryRepository.count();
        long roots = categoryRepository.countByType(CategoryType.ROOT);
        long intermediate = categoryRepository.countByType(CategoryType.INTERMEDIATE);
        long leaf = categoryRepository.countByType(CategoryType.LEAF);

        stats.put("total", total);
        stats.put("roots", roots);
        stats.put("intermediate", intermediate);
        stats.put("leaf", leaf);

        if (total > 0) {
            stats.put("message", "Данные для API тестов готовы");
        } else {
            stats.put("message", "Категории не созданы. Выполните POST /admin/categories/generate");
        }

        return ResponseEntity.ok(stats);
    }
}