package ru.daniil.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.daniil.core.entity.base.discount.OrderDiscount;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.exceptions.DiscountNotApplicableException;
import ru.daniil.core.response.discount.DiscountInfo;
import ru.daniil.core.sharedInterfaces.UserProvider;
import ru.daniil.order.mapper.OrderDiscountMapper;
import ru.daniil.order.service.orderDiscount.OrderDiscountProcessorService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orderDisscount")
@Tag(name = "Управление скидками заказа",
        description = "API для применения и удаления скидок с заказа пользователя")
public class OrderDiscountController {

    private final OrderDiscountProcessorService orderDiscountProcessorService;
    private final UserProvider userProvider;
    private final OrderDiscountMapper orderDiscountMapper;

    public OrderDiscountController(OrderDiscountProcessorService orderDiscountProcessorService,
                                   UserProvider userProvider,
                                   OrderDiscountMapper orderDiscountMapper) {
        this.orderDiscountProcessorService = orderDiscountProcessorService;
        this.userProvider = userProvider;
        this.orderDiscountMapper = orderDiscountMapper;
    }

    @PostMapping("/{orderNumber}/{discountCode}")
    @Operation(
            summary = "Применить скидку к заказу",
            description = "Применяет указанную скидку к заказу пользователя. " +
                    "Скидка будет применена только если она реально уменьшает итоговую сумму заказа."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Скидка успешно применена",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Скидка не применима к заказу или некорректные данные",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Торговля заблокирована для пользователя",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Заказ или скидка не найдены",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> applyDiscount(
            @Parameter(description = "Номер заказа", example = "ORD-1234567890-ABCD1234")
            @PathVariable String orderNumber,

            @Parameter(description = "Код скидки (промокод)", example = "SUMMER2024")
            @PathVariable String discountCode) {

        try {
            User user = userProvider.getAuthUser();
            OrderDiscount orderDiscount = orderDiscountProcessorService
                    .applyDiscountToOrder(orderNumber, discountCode, user);
            return ResponseEntity.ok(orderDiscount);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Некорректный запрос",
                            "message", e.getMessage()
                    ));
        } catch (DiscountNotApplicableException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Скидка не применима",
                            "message", e.getMessage()
                    ));
        }
    }

    @DeleteMapping("/{orderNumber}/{discountCode}")
    @Operation(
            summary = "Удалить скидку с заказа",
            description = "Удаляет ранее примененную скидку с заказа пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Скидка успешно удалена",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Заказ или скидка не найдены",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> removeDiscount(
            @Parameter(description = "Номер заказа", example = "ORD-1234567890-ABCD1234")
            @PathVariable String orderNumber,

            @Parameter(description = "Код скидки (промокод)", example = "SUMMER2024")
            @PathVariable String discountCode) {

        try {
            User user = userProvider.getAuthUser();
            orderDiscountProcessorService.removeDiscountFromOrder(orderNumber, discountCode, user);
            return ResponseEntity.ok()
                    .body(Map.of("message", "Скидка успешно удалена с заказа"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Некорректный запрос",
                            "message", e.getMessage()
                    ));
        }
    }

    @GetMapping("/{orderNumber}/discounts")
    @Operation(
            summary = "Получить примененные скидки заказа",
            description = "Возвращает список всех скидок, примененных к указанному заказу"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список скидок успешно получен",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный запрос",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Заказ не найден",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getAppliedDiscounts(
            @Parameter(description = "Номер заказа", example = "ORD-1234567890-ABCD1234")
            @PathVariable String orderNumber) {

        try {
            User user = userProvider.getAuthUser();
            List<DiscountInfo> discounts = orderDiscountMapper
                    .toOrderDiscountInfoList(
                            orderDiscountProcessorService.getAppliedDiscounts(orderNumber, user));
            return ResponseEntity.ok(discounts);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Некорректный запрос",
                            "message", e.getMessage()
                    ));
        }
    }
}
