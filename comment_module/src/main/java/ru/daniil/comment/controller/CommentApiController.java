package ru.daniil.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.daniil.comment.mapper.CommentMapper;
import ru.daniil.comment.service.CommentService;
import ru.daniil.core.entity.base.product.Comment;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.comment.CreateCommentRequest;
import ru.daniil.core.request.comment.UpdateCommentRequest;
import ru.daniil.core.response.comment.CommentResponse;
import ru.daniil.core.sharedInterfaces.UserProvider;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@Tag(name = "Api комментариев", description = "API для работы с комментариями")
public class CommentApiController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;
    private final UserProvider userProvider;

    public CommentApiController(CommentService commentService,
                                CommentMapper commentMapper, UserProvider userProvider) {
        this.commentService = commentService;
        this.commentMapper = commentMapper;
        this.userProvider = userProvider;
    }

    @PostMapping
    @Operation(
            summary = "Создание комментария",
            description = "Создаёт новый комментарий к продукту"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Комментарий успешно создан",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные или уже оставлен комментарий к этому товару",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Продукт не найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<CommentResponse> createComment(
            @Valid @RequestBody CreateCommentRequest request) {

        User user = userProvider.getAuthUser();
        Comment comment = commentService.createComment(request, user);
        CommentResponse response = commentMapper.toCommentResponse(comment, request.getProductSku(), user);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{commentId}")
    @Operation(
            summary = "Обновление комментария",
            description = "Обновляет существующий комментарий (только свои комментарии)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Комментарий успешно обновлён",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Попытка редактировать чужой комментарий",
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
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(description = "ID комментария", required = true)
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {

        User user = userProvider.getAuthUser();
        Comment comment = commentService.updateComment(commentId, request, user);
        CommentResponse response = commentMapper.toCommentResponse(
                comment,
                comment.getProduct().getSku(),
                user
        );

        return ResponseEntity.ok(response);
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
        commentService.deleteComment(commentId, user, false);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Комментарий успешно удалён");
        return ResponseEntity.ok(response);
    }
}