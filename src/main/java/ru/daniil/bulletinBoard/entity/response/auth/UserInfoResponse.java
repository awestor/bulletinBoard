package ru.daniil.bulletinBoard.entity.response.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.daniil.bulletinBoard.entity.base.user.Role;
import ru.daniil.bulletinBoard.entity.base.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ответ с информацией о пользователе
 * Не абстрактный, полноценный класс для передачи данных
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Long id;
    private String login;
    private String email;
    private Set<String> roles;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private boolean tradingBlocked;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate blockedUntil;

    /**
     * Конструктор для создания из сущности User
     */
    public static UserInfoResponse fromUser(User user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setLogin(user.getLogin());
        response.setEmail(user.getEmail());
        response.setRoles(user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
        response.setCreatedAt(user.getCreatedAt());
        response.setTradingBlocked(user.isTradingBlocked());
        response.setBlockedUntil(user.getBlockedUntil());
        return response;
    }

    /**
     * Конструктор с основными полями
     */
    public UserInfoResponse(Long id, String login, String email, Set<String> roles) {
        this.id = id;
        this.login = login;
        this.email = email;
        this.roles = roles;
        this.createdAt = LocalDateTime.now();
    }
}