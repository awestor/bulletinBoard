package ru.daniil.user.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.user.mapper.UserMapper;
import ru.daniil.user.service.user.UserAdminService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Api для действий администратора над пользователями"
        , description = "API для воздействия админов на пользователей")
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final UserMapper userMapper;

    public UserAdminController(UserAdminService userAdminService, UserMapper userMapper) {
        this.userAdminService = userAdminService;
        this.userMapper = userMapper;
    }

    @PutMapping("/users/{userId}/block")
    @Operation(
            summary = "Блокировка пользователя",
            description = "Блокирует пользователя до указанной даты"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно заблокирован"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "401", description = "Администратор не авторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<?> blockUser(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Дата окончания блокировки (ГГГГ-ММ-ДД)") LocalDate blockedUntil) {
        User updatedUser = userAdminService.blockUser(userId, blockedUntil);
        return ResponseEntity.ok(userMapper.toUserInfoResponse(updatedUser));
    }

    @PutMapping("/users/{userId}/unblock")
    @Operation(
            summary = "Разблокировка пользователя",
            description = "Снимает блокировку с пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно разблокирован"),
            @ApiResponse(responseCode = "401", description = "Администратор не авторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<?> unblockUser(@PathVariable Long userId) {
        User updatedUser = userAdminService.unblockUser(userId);
        return ResponseEntity.ok(userMapper.toUserInfoResponse(updatedUser));
    }

    @PutMapping("/users/{userId}/block-trading")
    @Operation(
            summary = "Блокировка торговых операций пользователя",
            description = "Запрещает пользователю совершать торговые операции"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Торговые операции успешно заблокированы"),
            @ApiResponse(responseCode = "401", description = "Администратор не авторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<?> blockTrading(@PathVariable Long userId) {
        User updatedUser = userAdminService.blockTrading(userId);
        return ResponseEntity.ok(userMapper.toUserInfoResponse(updatedUser));
    }

    @PutMapping("/users/{userId}/unblock-trading")
    @Operation(
            summary = "Разблокировка торговых операций пользователя",
            description = "Разрешает пользователю совершать торговые операции"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Торговые операции успешно разблокированы"),
            @ApiResponse(responseCode = "401", description = "Администратор не авторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<?> unblockTrading(@PathVariable Long userId) {
        User updatedUser = userAdminService.unblockTrading(userId);
        return ResponseEntity.ok(userMapper.toUserInfoResponse(updatedUser));
    }
}
