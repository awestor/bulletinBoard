package ru.daniil.comment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.daniil.core.entity.base.product.Comment;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.comment.CreateCommentRequest;
import ru.daniil.core.request.comment.UpdateCommentRequest;

import java.util.Optional;

public interface CommentService {

    Comment createComment(CreateCommentRequest request, User user);

    Comment updateComment(Long commentId, UpdateCommentRequest request, User user);

    void deleteComment(Long commentId, User user, boolean skipVerify);

    Comment getCommentById(Long commentId);

    Page<Comment> getCommentsByProductSku(String sku, Pageable pageable);

    Optional<Comment> getCommentsByUserAndProductSku(User user, String productSku);

    Double getAverageRatingByProductSku(String sku);
}
