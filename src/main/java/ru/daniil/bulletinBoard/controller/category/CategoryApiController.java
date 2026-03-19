package ru.daniil.bulletinBoard.controller.category;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.daniil.bulletinBoard.entity.base.product.Category;
import ru.daniil.bulletinBoard.service.category.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@Tag(name = "Api категорий", description = "API для работы с категориями")
public class CategoryApiController {

    private final CategoryService categoryService;

    public CategoryApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/by-name/{categoryName}")
    @Operation(summary = "Получение категории по имени", description = "Возвращает категорию по её имени")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категория найдена"),
            @ApiResponse(responseCode = "404", description = "Категория не найдена")
    })
    public ResponseEntity<Category> getCategoryByName(
            @Parameter(description = "Имя категории", required = true)
            @PathVariable String categoryName) {
        Category category = categoryService.getByName(categoryName);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/by-id/{id}")
    @Operation(summary = "Получение категории по ID", description = "Возвращает категорию по её идентификатору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категория найдена"),
            @ApiResponse(responseCode = "404", description = "Категория не найдена")
    })
    public ResponseEntity<Category> getCategoryById(
            @Parameter(description = "ID категории", required = true)
            @PathVariable Long id) {
        Category category = categoryService.getById(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/root")
    @Operation(summary = "Получение корневых категорий", description = "Возвращает список всех корневых категорий (без родителя)")
    @ApiResponse(responseCode = "200", description = "Список корневых категорий успешно получен")
    public ResponseEntity<List<Category>> getRootCategories() {
        List<Category> rootCategories = categoryService.getRootCategories();
        return ResponseEntity.ok(rootCategories);
    }

    @GetMapping("/leaf")
    @Operation(summary = "Получение листовых категорий", description = "Возвращает список всех конечных категорий (листьев)")
    @ApiResponse(responseCode = "200", description = "Список листовых категорий успешно получен")
    public ResponseEntity<List<Category>> getLeafCategories() {
        List<Category> leafCategories = categoryService.getLeafCategories();
        return ResponseEntity.ok(leafCategories);
    }

    @GetMapping("/{categoryName}/next")
    @Operation(summary = "Получение дочерних категорий",
            description = "Возвращает список следующих (дочерних) категорий для указанной категории")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список дочерних категорий успешно получен"),
            @ApiResponse(responseCode = "404", description = "Категория не найдена")
    })
    public ResponseEntity<List<Category>> getNextCategories(
            @Parameter(description = "Имя категории", required = true)
            @PathVariable String categoryName) {
        List<Category> nextCategories = categoryService.getNextCategories(categoryName);
        return ResponseEntity.ok(nextCategories);
    }
}