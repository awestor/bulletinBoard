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
import ru.daniil.core.entity.base.wallet.Wallet;
import ru.daniil.core.entity.base.wallet.WalletTransaction;
import ru.daniil.core.request.payment.DepositRequest;
import ru.daniil.core.response.wallet.WalletBalanceResponse;
import ru.daniil.core.response.wallet.WalletTransactionResponse;
import ru.daniil.core.sharedInterfaces.UserProvider;
import ru.daniil.order.mapper.WalletMapper;
import ru.daniil.order.service.wallet.WalletService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallet")
@Tag(name = "Api кошелька", description = "API для работы с кошельком пользователя")
public class WalletApiController {

    private final WalletService walletService;
    private final UserProvider userProvider;
    private final WalletMapper walletMapper;

    public WalletApiController(WalletService walletService,
                               UserProvider userProvider, WalletMapper walletMapper) {
        this.walletService = walletService;
        this.userProvider = userProvider;
        this.walletMapper = walletMapper;
    }

    @GetMapping("/balance")
    @Operation(
            summary = "Получение баланса кошелька",
            description = "Возвращает текущий баланс кошелька пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Баланс успешно получен",
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
    public ResponseEntity<WalletBalanceResponse> getBalance() {
        try {
            User user = getAuthenticatedUser();
            Wallet wallet = walletService.getOrCreateWallet(user);

            WalletBalanceResponse response = walletMapper.toBalanceResponse(
                    wallet, walletService.getAvailableBalance(wallet)
            );

            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/deposit")
    @Operation(
            summary = "Пополнение кошелька",
            description = "Пополняет баланс кошелька пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Кошелек успешно пополнен",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректная сумма пополнения",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Конфликт: депозит уже существует",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> deposit(
            @Valid @RequestBody DepositRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = getAuthenticatedUser();
            Wallet wallet = walletService.getOrCreateWallet(user);

            WalletTransaction transaction = walletService.deposit(
                    wallet,
                    request.getAmount(),
                    request.getExternalDepositId(),
                    request.getDescription() != null ? request.getDescription() : "Пополнение кошелька"
            );

            response.put("message", "Кошелек успешно пополнен");
            response.put("transactionId", transaction.getId());
            response.put("amount", request.getAmount());
            response.put("newBalance", wallet.getBalance());

            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException | BadRequestException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/transactions")
    @Operation(
            summary = "Получение истории транзакций",
            description = "Возвращает список всех транзакций кошелька пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "История транзакций успешно получена",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<List<WalletTransactionResponse>> getTransactions() {
        try {
            User user = getAuthenticatedUser();
            Wallet wallet = walletService.getOrCreateWallet(user);
            List<WalletTransaction> transactions = walletService.getTransactionsByWallet(wallet);

            List<WalletTransactionResponse> response = walletMapper.toTransactionResponseList(transactions);

            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/transactions/{transactionId}")
    @Operation(
            summary = "Получение транзакции по ID",
            description = "Возвращает информацию о конкретной транзакции"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Транзакция успешно найдена",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Транзакция не найдена",
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
    public ResponseEntity<WalletTransactionResponse> getTransaction(@PathVariable Long transactionId) {
        try {
            User user = getAuthenticatedUser();
            WalletTransaction transaction = walletService.getTransactionById(transactionId);

            if (!transaction.getWallet().getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            WalletTransactionResponse response = walletMapper.toTransactionResponse(transaction);

            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/frozen")
    @Operation(
            summary = "Получение активных заморозок",
            description = "Возвращает список активных замороженных транзакций"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список заморозок успешно получен",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<List<WalletTransactionResponse>> getActiveFreezes() {
        try {
            User user = getAuthenticatedUser();
            Wallet wallet = walletService.getOrCreateWallet(user);
            List<WalletTransaction> allTransactions = walletService.getTransactionsByWallet(wallet);

            List<WalletTransaction> freezes = allTransactions.stream()
                    .filter(t -> t.getTransactionType().name().equals("FREEZE"))
                    .filter(t -> t.getStatus().name().equals("COMPLETED"))
                    .collect(Collectors.toList());

            List<WalletTransactionResponse> response = walletMapper.toTransactionResponseList(freezes);

            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
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