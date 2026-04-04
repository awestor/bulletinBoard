package ru.daniil.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.auth.RegistrationRequest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "imageName", ignore = true)
    @Mapping(target = "blockedUntil", ignore = true)
    @Mapping(target = "tradingBlocked", constant = "false")
    @Mapping(target = "roles", ignore = true)
    User registrationRequestToUser(RegistrationRequest request);
}