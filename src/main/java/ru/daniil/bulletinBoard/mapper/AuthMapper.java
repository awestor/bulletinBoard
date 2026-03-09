package ru.daniil.bulletinBoard.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.entity.request.auth.RegistrationRequest;
import ru.daniil.bulletinBoard.entity.response.jwt.JwtResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "imageName", ignore = true)
    @Mapping(target = "blockedUntil", ignore = true)
    @Mapping(target = "tradingBlocked", constant = "false")
    @Mapping(target = "roles", ignore = true)
    User registrationRequestToUser(RegistrationRequest request);

    @Mapping(target = "token", source = "token")
    @Mapping(target = "type", constant = "Bearer")
    JwtResponse toJwtResponse(String token);
}