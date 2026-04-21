package ru.daniil.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.orderItem.CreateOrderItemRequest;
import ru.daniil.core.request.orderItem.DeleteOrderItemRequest;
import ru.daniil.core.request.orderItem.ReduceQuantityRequest;
import ru.daniil.core.sharedInterfaces.UserProvider;
import ru.daniil.order.service.order.OrderProcessorService;
import ru.daniil.order.service.order.OrderService;
import ru.daniil.order.service.orderItem.OrderItemService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order-items")
@Tag(name = "Api товаров заказа", description = "API для работы с товарами в заказе")
public class OrderItemApiController {

    private final OrderProcessorService orderProcessorService;
    private final OrderItemService orderItemService;
    private final UserProvider userProvider;
    private final OrderService orderService;

    public OrderItemApiController(OrderProcessorService orderProcessorService,
                                  OrderItemService orderItemService,
                                  UserProvider userProvider, OrderService orderService) {
        this.orderProcessorService = orderProcessorService;
        this.orderItemService = orderItemService;
        this.userProvider = userProvider;
        this.orderService = orderService;
    }

    @PostMapping("/add")
    @Operation(
            summary = "Добавление товара в заказ",
            description = "Добавляет товар в текущий черновик заказа"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Товар успешно добавлен в заказ",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный запрос или недостаточно товара на складе",
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
                    responseCode = "409",
                    description = "Недостаточно средств на кошельке",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> addOrderItem(
            @Valid @RequestBody CreateOrderItemRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = userProvider.getAuthUser();
            OrderItem orderItem = orderProcessorService.addOrderItem(request, user);

            response.put("message", "Товар успешно добавлен в заказ");
            response.put("orderItem", orderItem);
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException | BadRequestException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalStateException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    @DeleteMapping("/remove")
    @Operation(
            summary = "Удаление товара из заказа",
            description = "Удаляет товар из текущего черновика заказа по SKU"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Товар успешно удален из заказа",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный запрос",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Товар не найден в заказе",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> removeOrderItem(
            @Valid @RequestBody DeleteOrderItemRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = userProvider.getAuthUser();
            orderProcessorService.removeOrderItem(request, user);

            response.put("message", "Товар успешно удален из заказа");
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalArgumentException | BadRequestException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/reduce")
    @Operation(
            summary = "Уменьшение количества товара из заказа",
            description = "Уменьшает количество товара из текущего черновика заказа по SKU"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Товар успешно изменён в заказе",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный запрос",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Товар не найден в заказе",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> removeOrderItem(
            @Valid @RequestBody ReduceQuantityRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = userProvider.getAuthUser();
            orderProcessorService.reduceQuantityOrderItem(request, user);

            response.put("message", "Количество товара успешно изменено");
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalArgumentException | BadRequestException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/order/{orderNumber}")
    @Operation(
            summary = "Получение всех товаров заказа",
            description = "Возвращает список всех товаров в указанном заказе"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список товаров успешно получен",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Заказ не найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Доступ запрещен",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<List<OrderItem>> getOrderItems(@PathVariable String orderNumber) {
        User user = userProvider.getAuthUser();

        Order order = orderService.getByOrderNumber(orderNumber);
        if (!order.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<OrderItem> orderItems = orderItemService.getByOrderNumber(orderNumber);
        return ResponseEntity.ok(orderItems);
    }

    @GetMapping("/reservations/{sku}")
    @Operation(
            summary = "Получение количества зарезервированных товаров",
            description = "Возвращает общее количество единиц товара, которое сейчас зарезервировано " +
                    "в корзинах пользователей"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Количество успешно получено",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный SKU",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Товар с указанным SKU не найден",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Integer> getReservationCount(
            @Parameter(description = "Артикул продукта (SKU)", required = true)
            @PathVariable String sku) {
        try {
            Integer reservationCount = orderItemService.countReservation(sku);
            return ResponseEntity.ok(reservationCount);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}