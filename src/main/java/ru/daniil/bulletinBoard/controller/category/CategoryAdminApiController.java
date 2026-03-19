package ru.daniil.bulletinBoard.controller.category;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.daniil.bulletinBoard.entity.base.product.Category;
import ru.daniil.bulletinBoard.entity.request.CreateCategoryRequest;
import ru.daniil.bulletinBoard.service.category.CategoryService;

@RestController
@RequestMapping("/api/admin/category")
@Tag(name = "Api категорий для администратора", description = "API для работы с категориями с ограничением доступа")
public class CategoryAdminApiController {
    private final CategoryService categoryService;

    public CategoryAdminApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }


    @PostMapping
    @Operation(summary = "Создание новой категории", description = "Создает новую категорию на основе переданных данных")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Категория успешно создана"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "409", description = "Категория с таким именем уже существует")
    })
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        Category createdCategory = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    @DeleteMapping("/{categoryName}")
    @Operation(summary = "Удаление категории", description = "Удаляет категорию по её имени")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Категория успешно удалена"),
            @ApiResponse(responseCode = "404", description = "Категория не найдена"),
            @ApiResponse(responseCode = "409", description = "Невозможно удалить категорию, так как у неё есть дочерние категории")
    })
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Имя категории", required = true)
            @PathVariable String categoryName) {
        categoryService.delete(categoryName);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{categoryOldName}")
    @Operation(summary = "Обновление категории", description = "Обновляет существующую категорию по старому имени")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категория успешно обновлена"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "404", description = "Категория не найдена"),
            @ApiResponse(responseCode = "409", description = "Категория с таким именем уже существует")
    })
    public ResponseEntity<Category> updateCategory(
            @Parameter(description = "Старое имя категории", required = true)
            @PathVariable String categoryOldName,
            @Valid @RequestBody CreateCategoryRequest request) {
        Category updatedCategory = categoryService.update(categoryOldName, request);
        return ResponseEntity.ok(updatedCategory);
    }

    @PatchMapping("/{categoryId}/update-parent")
    @Operation(summary = "Обновление родителя для категорий",
            description = "Переносит все дочерние категории из одной категории в другую")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Родительские связи успешно обновлены"),
            @ApiResponse(responseCode = "404", description = "Одна из категорий не найдена"),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос")
    })
    public ResponseEntity<Void> updateParentForCategories(
            @Parameter(description = "ID исходной категории", required = true)
            @RequestParam Long sourceCategoryId,
            @Parameter(description = "ID целевой категории (нового родителя)", required = true)
            @RequestParam Long targetCategoryId) {

        Category sourceCategory = categoryService.getById(sourceCategoryId);
        Category targetCategory = categoryService.getById(targetCategoryId);

        categoryService.updateParentForCategories(sourceCategory, targetCategory);
        return ResponseEntity.ok().build();
    }
}
