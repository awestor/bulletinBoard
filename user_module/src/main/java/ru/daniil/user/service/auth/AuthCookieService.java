package ru.daniil.user.service.auth;

import jakarta.servlet.http.HttpServletResponse;

public interface AuthCookieService {
    void setAuthCookies(HttpServletResponse response, String accessToken, String username);

    void clearAuthCookies(HttpServletResponse response);

    void updateUsernameCookie(HttpServletResponse response, String username);

    void updateTokenCookie(HttpServletResponse response, String accessToken);
}
