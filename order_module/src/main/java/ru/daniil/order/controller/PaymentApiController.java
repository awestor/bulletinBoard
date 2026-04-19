package ru.daniil.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.payment.UpdateStatusPaymentRequest;
import ru.daniil.core.response.payment.PaymentCompleteResponse;
import ru.daniil.core.response.payment.PaymentInitiateResponse;
import ru.daniil.core.sharedInterfaces.UserProvider;
import ru.daniil.order.service.payment.PaymentProcessorService;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Api оплаты", description = "API для работы с оплатой заказов")
public class PaymentApiController {

    private final PaymentProcessorService paymentProcessorService;
    private final UserProvider userProvider;

    public PaymentApiController(PaymentProcessorService paymentProcessorService,
                                UserProvider userProvider) {
        this.paymentProcessorService = paymentProcessorService;
        this.userProvider = userProvider;
    }

    @PostMapping("/initiate")
    @Operation(
            summary = "Инициализация оплаты",
            description = "Этап 1: Заморозка средств и списание товаров со склада"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Оплата успешно инициализирована",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный запрос или недостаточно средств",
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
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Конфликт: товары недоступны или заказ уже в обработке",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<PaymentInitiateResponse> initiatePayment() {
        try {
            User user = getAuthenticatedUser();
            PaymentInitiateResponse response = paymentProcessorService.initiatePayment(user);
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException | IllegalArgumentException | BadRequestException e) {
            PaymentInitiateResponse errorResponse = PaymentInitiateResponse.builder()
                    .status("ERROR")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
    }

    @PostMapping("/complete")
    @Operation(
            summary = "Подтверждение оплаты",
            description = "Этап 2: Списание замороженных средств и завершение заказа"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Оплата успешно завершена",
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
                    description = "Заказ или транзакция не найдены",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Конфликт: заказ не в статусе ожидания оплаты",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<PaymentCompleteResponse> completePayment(
            @Valid @RequestBody UpdateStatusPaymentRequest request) {
        try {
            User user = getAuthenticatedUser();
            PaymentCompleteResponse response = paymentProcessorService.completePayment(request, user);
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            PaymentCompleteResponse errorResponse = PaymentCompleteResponse.builder()
                    .status("ERROR")
                    .message("Заказ или транзакция не найдены")
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (IllegalStateException | SecurityException e) {
            PaymentCompleteResponse errorResponse = PaymentCompleteResponse.builder()
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
    }

    @PostMapping("/cancel/{orderNumber}")
    @Operation(
            summary = "Отмена оплаты",
            description = "Отмена инициализированной оплаты: разморозка средств и возврат товаров на склад"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Оплата успешно отменена",
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
                    description = "Заказ или транзакция не найдены",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Конфликт: заказ не в статусе ожидания оплаты",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<PaymentCompleteResponse> cancelPayment(
            @Valid @RequestBody UpdateStatusPaymentRequest request) {
        try {
            User user = getAuthenticatedUser();
            PaymentCompleteResponse response = paymentProcessorService.cancelPayment(request, user);
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            PaymentCompleteResponse errorResponse = PaymentCompleteResponse.builder()
                    .status("ERROR")
                    .message("Заказ или транзакция не найдены")
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (BadRequestException | IllegalStateException e) {
            PaymentCompleteResponse errorResponse = PaymentCompleteResponse.builder()
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
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
