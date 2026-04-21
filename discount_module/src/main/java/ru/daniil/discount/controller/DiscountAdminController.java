package ru.daniil.discount.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.request.discount.CreateDiscountRequest;
import ru.daniil.core.response.discount.DiscountInfo;
import ru.daniil.discount.mapper.DiscountMapper;
import ru.daniil.discount.service.DiscountService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/discounts")
@Tag(name = "Управление скидками доступное администратору",
        description = "API для создания и управления скидками в системе")
public class DiscountAdminController {
    private final DiscountService discountService;
    private final DiscountMapper discountMapper;

    public DiscountAdminController(DiscountService discountService,
                                   DiscountMapper discountMapper) {
        this.discountService = discountService;
        this.discountMapper = discountMapper;
    }

    @PostMapping
    @Operation(
            summary = "Создание новой скидки",
            description = "Создает новую скидку в системе. Доступно только администраторам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Скидка успешно создана",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса или скидка с таким кодом уже существует",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> createDiscount(@Valid @RequestBody CreateDiscountRequest request) {
        try {
            Discount discount = discountService.createDiscount(request);
            return ResponseEntity.ok(discount);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Ошибка создания скидки",
                            "message", e.getMessage()
                    ));
        }
    }
    @PatchMapping("/{code}/deactivate")
    @Operation(
            summary = "Деактивация скидки",
            description = "Делает скидку неактивной (мягкое удаление)." +
                    " Скидка больше не будет применяться к новым заказам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Скидка успешно деактивирована"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Скидка не найдена"
            )
    })
    public ResponseEntity<?> deactivateDiscount(
            @Parameter(description = "Код скидки", example = "SUMMER2024")
            @PathVariable String code) {
        try {
            discountService.deactivateDiscount(code);
            return ResponseEntity.ok()
                    .body(Map.of("message", "Скидка успешно деактивирована"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Ошибка деактивации",
                            "message", e.getMessage()
                    ));
        }
    }

    @PatchMapping("/{code}/activate")
    @Operation(
            summary = "Активация скидки",
            description = "Делает ранее деактивированную скидку активной"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Скидка успешно активирована"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Скидка не найдена"
            )
    })
    public ResponseEntity<?> activateDiscount(
            @Parameter(description = "Код скидки", example = "SUMMER2024")
            @PathVariable String code,
            @Parameter(description = "Код скидки", example = "SUMMER2024")
            @RequestBody Integer usages) {
        try {
            discountService.activateDiscount(code, usages);
            return ResponseEntity.ok()
                    .body(Map.of("message", "Скидка успешно активирована"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Ошибка активации",
                            "message", e.getMessage()
                    ));
        }
    }

    @GetMapping("/{code}")
    @Operation(
            summary = "Получение скидки по коду",
            description = "Возвращает полную информацию о скидке"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Скидка найдена",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Администратор не авторизован"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Скидка не найдена"
            )
    })
    public ResponseEntity<?> getDiscountByCode(
            @Parameter(description = "Код скидки", example = "SUMMER2024")
            @PathVariable String code) {
        try {
            Discount discount = discountService.getDiscountByCode(code);
            return ResponseEntity.ok(discount);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Скидка не найдена",
                            "message", e.getMessage()
                    ));
        }
    }

    @GetMapping
    @Operation(
            summary = "Получение списка всех скидок",
            description = "Возвращает список всех скидок в системе (не включая неактивные)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список скидок успешно получен"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Администратор не авторизован"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            )
    })
    public ResponseEntity<?> getAllDiscounts() {
        try {
            List<DiscountInfo> discounts = discountMapper.toDiscountInfoList(
                    discountService.getAllActiveDiscount());
            return ResponseEntity.ok(discounts);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Ошибка получения списка",
                            "message", e.getMessage()
                    ));
        }
    }
}
