package ru.daniil.comment.commentService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.daniil.comment.repository.CommentRepository;
import ru.daniil.comment.service.CommentServiceImpl;
import ru.daniil.core.entity.base.product.Comment;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.comment.CreateCommentRequest;
import ru.daniil.core.request.comment.UpdateCommentRequest;
import ru.daniil.product.service.product.ProductService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User testUser;
    private Product testProduct;
    private Comment testComment;
    private CreateCommentRequest createRequest;
    private UpdateCommentRequest updateRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .login("testUser")
                .email("test@example.com")
                .password("password")
                .build();

        testProduct = Product.builder()
                .id(1L)
                .sku("TEST-SKU-001")
                .name("Test Product")
                .price(java.math.BigDecimal.valueOf(100))
                .seller(testUser)
                .build();

        testComment = Comment.builder()
                .id(1L)
                .content("Great product!")
                .rating(5)
                .product(testProduct)
                .user(testUser)
                .build();

        createRequest = CreateCommentRequest.builder()
                .content("Great product!")
                .rating(5)
                .productSku("TEST-SKU-001")
                .build();

        updateRequest = UpdateCommentRequest.builder()
                .content("Updated comment")
                .rating(4)
                .build();

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void createComment_Success() {
        when(productService.getBySku("TEST-SKU-001")).thenReturn(testProduct);
        when(commentRepository.existsByUserIdAndProductId(testUser.getId(), testProduct.getId()))
                .thenReturn(false);
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        Comment result = commentService.createComment(createRequest, testUser);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Great product!");
        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getProduct()).isEqualTo(testProduct);
        assertThat(result.getUser()).isEqualTo(testUser);
    }

    @Test
    void createComment_UserAlreadyCommented_ThrowsBadRequestException() {
        when(productService.getBySku("TEST-SKU-001")).thenReturn(testProduct);
        when(commentRepository.existsByUserIdAndProductId(testUser.getId(), testProduct.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> commentService.createComment(createRequest, testUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Лимит на количество комментариев к товару составляет: 1 комментарий.");

        verify(productService).getBySku("TEST-SKU-001");
        verify(commentRepository).existsByUserIdAndProductId(testUser.getId(), testProduct.getId());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void updateComment_Success() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        Comment result = commentService.updateComment(1L, updateRequest, testUser);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Updated comment");
        assertThat(result.getRating()).isEqualTo(4);

        verify(commentRepository).findById(1L);
        verify(commentRepository).save(testComment);
    }

    @Test
    void updateComment_PartialUpdate_OnlyContent() {
        UpdateCommentRequest partialRequest = UpdateCommentRequest.builder()
                .content("Only content updated")
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        Comment result = commentService.updateComment(1L, partialRequest, testUser);

        assertThat(result.getContent()).isEqualTo("Only content updated");
        assertThat(result.getRating()).isEqualTo(5);
    }

    @Test
    void updateComment_PartialUpdate_OnlyRating() {
        UpdateCommentRequest partialRequest = UpdateCommentRequest.builder()
                .rating(3)
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        Comment result = commentService.updateComment(1L, partialRequest, testUser);

        assertThat(result.getContent()).isEqualTo("Great product!"); // Content unchanged
        assertThat(result.getRating()).isEqualTo(3);
    }

    @Test
    void updateComment_CommentNotFound_ThrowsEntityNotFoundException() {
        // given
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.updateComment(999L, updateRequest, testUser))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Комментарий не найден с id: 999");

        verify(commentRepository).findById(999L);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void updateComment_UserNotOwner_ThrowsSecurityException() {
        User anotherUser = User.builder()
                .id(2L)
                .login("anotherUser")
                .email("another@example.com")
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        assertThatThrownBy(() -> commentService.updateComment(1L, updateRequest, anotherUser))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Редактировать можно только свои комментарии");
    }

    @Test
    void deleteComment_Success() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        doNothing().when(commentRepository).delete(testComment);

        commentService.deleteComment(1L, testUser, false);

        verify(commentRepository).findById(1L);
        verify(commentRepository).delete(testComment);
    }

    @Test
    void deleteComment_CommentNotFound_ThrowsEntityNotFoundException() {
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService
                .deleteComment(999L, testUser, false))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Комментарий не найден с id: 999");

        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    void deleteComment_UserNotOwner_ThrowsSecurityException() {
        User anotherUser = User.builder()
                .id(2L)
                .login("anotherUser")
                .email("another@example.com")
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        assertThatThrownBy(() -> commentService.deleteComment(1L, anotherUser, false))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Удалять можно только свои комментарии");

        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    void getCommentById_Success() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        Comment result = commentService.getCommentById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("Great product!");
    }

    @Test
    void getCommentById_NotFound_ThrowsEntityNotFoundException() {
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getCommentById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Комментарий не найден с id: 999");
    }

    @Test
    void getCommentsByProductSku_Success() {
        List<Comment> comments = List.of(testComment);
        Page<Comment> commentPage = new PageImpl<>(comments, pageable, comments.size());

        when(commentRepository.findByProductSku("TEST-SKU-001", pageable)).thenReturn(commentPage);

        Page<Comment> result = commentService.getCommentsByProductSku("TEST-SKU-001", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent().size() == 1);
        assertThat(result.getContent().getFirst().getProduct().getSku()).isEqualTo("TEST-SKU-001");

        verify(commentRepository).findByProductSku("TEST-SKU-001", pageable);
    }

    @Test
    void getCommentsByProductSku_EmptyResult() {
        Page<Comment> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(commentRepository.findByProductSku("NONEXISTENT-SKU", pageable)).thenReturn(emptyPage);

        Page<Comment> result = commentService.getCommentsByProductSku("NONEXISTENT-SKU", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent().isEmpty());
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(commentRepository).findByProductSku("NONEXISTENT-SKU", pageable);
    }

    @Test
    void getCommentsByUserAndProductSku_Success() {
        when(commentRepository.findByUserAndProductSku(testUser, "TEST-SKU-001"))
                .thenReturn(Optional.of(testComment));

        Optional<Comment> result = commentService.getCommentsByUserAndProductSku(testUser, "TEST-SKU-001");

        assertThat(result).isPresent();
        assertThat(result.get().getUser()).isEqualTo(testUser);
        assertThat(result.get().getProduct().getSku()).isEqualTo("TEST-SKU-001");

        verify(commentRepository).findByUserAndProductSku(testUser, "TEST-SKU-001");
    }

    @Test
    void getCommentsByUserAndProductSku_NotFound() {
        when(commentRepository.findByUserAndProductSku(testUser, "TEST-SKU-001"))
                .thenReturn(Optional.empty());

        Optional<Comment> result = commentService.getCommentsByUserAndProductSku(testUser, "TEST-SKU-001");

        assertThat(result).isEmpty();

        verify(commentRepository).findByUserAndProductSku(testUser, "TEST-SKU-001");
    }

    @Test
    void getAverageRatingByProductSku_Success() {
        when(productService.getBySku("TEST-SKU-001")).thenReturn(testProduct);
        when(commentRepository.getAverageRatingByProductId(testProduct.getId())).thenReturn(4.5);

        Double result = commentService.getAverageRatingByProductSku("TEST-SKU-001");

        assertThat(result).isEqualTo(4.5);

        verify(productService).getBySku("TEST-SKU-001");
        verify(commentRepository).getAverageRatingByProductId(testProduct.getId());
    }

    @Test
    void getAverageRatingByProductSku_NoRatings_ReturnsZero() {
        when(productService.getBySku("TEST-SKU-001")).thenReturn(testProduct);
        when(commentRepository.getAverageRatingByProductId(testProduct.getId())).thenReturn(null);

        Double result = commentService.getAverageRatingByProductSku("TEST-SKU-001");

        assertThat(result).isEqualTo(0.0);

        verify(productService).getBySku("TEST-SKU-001");
        verify(commentRepository).getAverageRatingByProductId(testProduct.getId());
    }

    @Test
    void getAverageRatingByProductSku_ProductNotFound_ThrowsException() {
        when(productService.getBySku("NONEXISTENT-SKU")).thenThrow(new NotFoundException("Product not found"));

        assertThatThrownBy(() -> commentService.getAverageRatingByProductSku("NONEXISTENT-SKU"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product not found");

        verify(productService).getBySku("NONEXISTENT-SKU");
        verify(commentRepository, never()).getAverageRatingByProductId(any());
    }
}
