package ru.daniil.comment.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.daniil.comment.repository.CommentRepository;
import ru.daniil.core.entity.base.product.Comment;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.comment.CreateCommentRequest;
import ru.daniil.core.request.comment.UpdateCommentRequest;
import ru.daniil.product.service.product.ProductService;

import java.util.Optional;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ProductService productService;

    public CommentServiceImpl(CommentRepository commentRepository,
                              ProductService productService) {
        this.commentRepository = commentRepository;
        this.productService = productService;
    }

    @Override
    @Transactional
    public Comment createComment(CreateCommentRequest request, User user) {
        Product product = productService.getBySku(request.getProductSku());

        if (commentRepository.existsByUserIdAndProductId(user.getId(), product.getId())) {
            throw new BadRequestException("Лимит на количество комментариев к товару составляет: 1 комментарий.");
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .rating(request.getRating())
                .product(product)
                .user(user)
                .build();

        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public Comment updateComment(Long commentId, UpdateCommentRequest request, User user) {
        Comment comment = getCommentEntityById(commentId);

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Редактировать можно только свои комментарии");
        }

        if (request.getContent() != null) {
            comment.setContent(request.getContent());
        }
        if (request.getRating() != null) {
            comment.setRating(request.getRating());
        }

        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, User user, boolean skipVerify) {
        Comment comment = getCommentEntityById(commentId);

        if (!comment.getUser().getId().equals(user.getId()) || skipVerify) {
            throw new SecurityException("Удалять можно только свои комментарии");
        }

        commentRepository.delete(comment);
    }

    @Override
    public Comment getCommentById(Long commentId) {
        return getCommentEntityById(commentId);
    }

    @Override
    public Page<Comment> getCommentsByProductSku(String sku, Pageable pageable) {
        return commentRepository.findByProductSku(sku, pageable);
    }

    @Override
    public Optional<Comment> getCommentsByUserAndProductSku(User user, String productSku) {
        return commentRepository.findByUserAndProductSku(user, productSku);
    }

    @Override
    public Double getAverageRatingByProductSku(String sku) {
        Product product = productService.getBySku(sku);
        Double avgRating = commentRepository.getAverageRatingByProductId(product.getId());
        return avgRating != null ? avgRating : 0.0;
    }

    private Comment getCommentEntityById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Комментарий не найден с id: " + commentId));
    }
}