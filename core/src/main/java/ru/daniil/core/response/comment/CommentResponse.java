package ru.daniil.core.response.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private Integer rating;
    private String productSku;
    private String authorLogin;
    private String authorEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}