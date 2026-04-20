package ru.daniil.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.daniil.comment.mapper.CommentMapper;
import ru.daniil.comment.service.CommentService;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.sharedInterfaces.UserProvider;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/comments")
@Tag(name = "Api комментариев для администратора", description = "API для работы с комментариями для админов")
public class CommentAdminApiController {
    private final CommentService commentService;
    private final UserProvider userProvider;

    public CommentAdminApiController(CommentService commentService,
                                UserProvider userProvider) {
        this.commentService = commentService;
        this.userProvider = userProvider;
    }

    @DeleteMapping("/{commentId}")
    @Operation(
            summary = "Удаление комментария",
            description = "Удаляет комментарий (только свои комментарии)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Комментарий успешно удалён",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Попытка удалить чужой комментарий",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Комментарий не найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> deleteComment(
            @Parameter(description = "ID комментария", required = true)
            @PathVariable Long commentId) {

        User user = userProvider.getAuthUser();
        commentService.deleteComment(commentId, user, true);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Комментарий успешно удалён");
        return ResponseEntity.ok(response);
    }
}
