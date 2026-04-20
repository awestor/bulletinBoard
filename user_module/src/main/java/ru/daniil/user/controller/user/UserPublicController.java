package ru.daniil.user.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.response.auth.UserInfoResponse;
import ru.daniil.user.mapper.UserMapper;
import ru.daniil.user.service.user.UserService;

import java.util.Map;

public class UserPublicController {
    private final UserService userService;
    private final UserMapper userMapper;

    public UserPublicController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/{commentId}/user")
    @Operation(
            summary = "Получение автора комментария",
            description = "Возвращает пользователя, который написал комментарий по его ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Комментарий или пользователь не найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getUserByCommentId(
            @Parameter(description = "ID комментария", required = true)
            @PathVariable Long commentId) {

        try {
            User user = userService.getUserByCommentId(commentId);
            UserInfoResponse userResponse = userMapper.toUserInfoResponse(user);
            return ResponseEntity.ok(userResponse);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Ошибка при получении пользователя: " + e.getMessage()));
        }
    }
}
