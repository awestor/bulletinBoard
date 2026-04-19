package ru.daniil.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.response.OrderResponse;
import ru.daniil.core.sharedInterfaces.UserProvider;
import ru.daniil.order.mapper.OrderMapper;
import ru.daniil.order.service.order.OrderService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Api заказов", description = "API для работы с заказами")
public class OrderApiController {

    private final OrderService orderService;
    private final UserProvider userProvider;
    private final OrderMapper orderMapper;

    public OrderApiController(OrderService orderService,
                              UserProvider userProvider, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.userProvider = userProvider;
        this.orderMapper = orderMapper;
    }

    @GetMapping("/current")
    @Operation(
            summary = "Получение текущего черновика заказа",
            description = "Возвращает текущий черновик заказа текущего пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Заказ успешно получен",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<OrderResponse> getCurrentOrder() {
        try {
            User user = getAuthenticatedUser();
            Order order = orderService.getLastOrCreateOrderByUser(user);
            return ResponseEntity.ok(orderMapper.toResponse(order));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{orderNumber}")
    @Operation(
            summary = "Получение заказа по номеру",
            description = "Возвращает заказ по его уникальному номеру"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Заказ успешно получен",
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
            )
    })
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            User user = getAuthenticatedUser();
            Order order = orderService.getByOrderNumber(orderNumber);

            if (!order.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(orderMapper.toResponse(order));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/history")
    @Operation(
            summary = "Получение истории заказов",
            description = "Возвращает список всех заказов текущего пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "История заказов успешно получена",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<List<OrderResponse>> getOrderHistory() {
        User user = getAuthenticatedUser();
        List<Order> orders = orderService.getByUser(user);
        return ResponseEntity.ok(orderMapper.toResponseList(orders));
    }

    @DeleteMapping("/{orderId}")
    @Operation(
            summary = "Удаление заказа",
            description = "Удаляет заказ по ID (только для черновиков)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Заказ успешно удален",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невозможно удалить завершенный заказ",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Заказ не найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Доступ запрещен",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> deleteOrder(@PathVariable Long orderId) {
        User user = getAuthenticatedUser();
        Map<String, Object> response = new HashMap<>();

        try {
            Order order = orderService.getById(orderId);

            if (!order.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            orderService.delete(orderId);
            response.put("message", "Заказ успешно удален");

            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BadRequestException | IllegalStateException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private User getAuthenticatedUser() {
        JwtAuthenticationToken authentication = (JwtAuthenticationToken)
                SecurityContextHolder.getContext().getAuthentication();

        assert authentication != null;
        Jwt jwt = authentication.getToken();
        String email = jwt.getClaimAsString("email");

        return userProvider.getByEmail(email).orElseThrow(
                () -> new NotFoundException("Пользователь не был аутентифицирован в системе")
        );
    }
}