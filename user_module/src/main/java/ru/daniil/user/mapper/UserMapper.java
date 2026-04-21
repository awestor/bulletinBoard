package ru.daniil.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.response.auth.UserInfoResponse;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "login", source  = "user.login")
    @Mapping(target = "email", source  = "user.email")
    @Mapping(target = "createdAt", source  = "user.createdAt")
    @Mapping(target = "blockedUntil", source  = "user.blockedUntil")
    @Mapping(target = "tradingBlocked", source = "tradingBlocked")
    UserInfoResponse toUserInfoResponse (User user);
}