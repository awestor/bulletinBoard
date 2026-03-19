package ru.daniil.bulletinBoard.service.user.auth;

import jakarta.servlet.http.HttpServletResponse;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.entity.response.auth.JwtResponse;

import java.util.Optional;

public interface KeycloakService {
    JwtResponse authenticateAndGetTokens(String username, String rawPassword, HttpServletResponse response);

    JwtResponse refreshAccessToken(String refreshToken, HttpServletResponse response);

    void createUserInKeycloak(User user, String rawPassword, HttpServletResponse response);

    void clearRefreshTokenCookie(HttpServletResponse response);

    void logout(String refreshToken, HttpServletResponse response);

    String getAdminToken();

    Optional<String> findUserInKeycloak(String username, String adminToken);

    JwtResponse exchange(String code);
}
