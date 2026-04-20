package ru.daniil.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.daniil.comment.mapper.CommentMapper;
import ru.daniil.comment.service.CommentService;
import ru.daniil.core.entity.base.product.Comment;
import ru.daniil.core.response.comment.CommentResponse;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/comments")
@Tag(name = "Api комментариев (без авторизации)",
        description = "API для работы с комментариями не требующими авторизации")
public class CommentPublicApiController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    public CommentPublicApiController(CommentService commentService,
                                      CommentMapper commentMapper) {
        this.commentService = commentService;
        this.commentMapper = commentMapper;
    }

    @GetMapping("/{commentId}")
    @Operation(
            summary = "Получение комментария по ID",
            description = "Возвращает комментарий по его идентификатору"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Комментарий найден",
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
    public ResponseEntity<CommentResponse> getCommentById(
            @Parameter(description = "ID комментария", required = true)
            @PathVariable Long commentId) {

        Comment comment = commentService.getCommentById(commentId);
        CommentResponse response = commentMapper.toCommentResponse(
                comment,
                comment.getProduct().getSku(),
                comment.getUser()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{sku}")
    @Operation(
            summary = "Получение комментариев по SKU продукта",
            description = "Возвращает страницу комментариев для указанного продукта"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Комментарии найдены",
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
    public ResponseEntity<Page<CommentResponse>> getCommentsByProductSku(
            @Parameter(description = "SKU продукта", required = true)
            @PathVariable String sku,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Comment> comments = commentService.getCommentsByProductSku(sku, pageable);
        Page<CommentResponse> response = comments.map(comment ->
                commentMapper.toCommentResponse(comment, sku, comment.getUser())
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{sku}/rating")
    @Operation(
            summary = "Получение среднего рейтинга продукта",
            description = "Возвращает средний рейтинг для указанного продукта"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Средний рейтинг получен",
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
    public ResponseEntity<Map<String, Object>> getAverageRating(
            @Parameter(description = "SKU продукта", required = true)
            @PathVariable String sku) {

        Double averageRating = commentService.getAverageRatingByProductSku(sku);

        Map<String, Object> response = new HashMap<>();
        response.put("sku", sku);
        response.put("averageRating", averageRating);
        return ResponseEntity.ok(response);
    }
}
