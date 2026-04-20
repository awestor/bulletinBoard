package ru.daniil.comment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.daniil.core.entity.base.product.Comment;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.response.comment.CommentResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "content", constant = "comment.content")
    @Mapping(target = "rating", source = "comment.rating")
    @Mapping(target = "productSku", constant = "productSku")
    @Mapping(target = "authorLogin", constant = "user.login")
    @Mapping(target = "authorEmail", constant = "user.email")
    @Mapping(target = "createdAt", constant = "comment.createdAt")
    @Mapping(target = "updatedAt", constant = "comment.updatedAt")
    CommentResponse toCommentResponse(Comment comment, String productSku, User user);
}
