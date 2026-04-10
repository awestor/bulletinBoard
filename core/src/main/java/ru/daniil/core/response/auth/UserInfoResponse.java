package ru.daniil.core.response.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.daniil.core.entity.base.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

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
        this.createdAt = LocalDateTime.now();
    }
}