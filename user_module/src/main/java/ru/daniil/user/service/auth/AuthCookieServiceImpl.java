package ru.daniil.user.service.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieServiceImpl implements AuthCookieService {

    @Value("${app.cookie.max-age:3600}")
    private int maxAge;

    private static final String TOKEN_COOKIE = "ACCESS_TOKEN";
    private static final String USERNAME_COOKIE = "USERNAME";

    /**
     * Установка cookie после успешного входа
     */
    public void setAuthCookies(HttpServletResponse response, String accessToken, String username) {
        setCookie(response, TOKEN_COOKIE, accessToken, true);
        setCookie(response, USERNAME_COOKIE, username, false);
    }

    /**
     * Удаление cookie при выходе
     */
    public void clearAuthCookies(HttpServletResponse response) {
        clearCookie(response, TOKEN_COOKIE);
        clearCookie(response, USERNAME_COOKIE);
    }

    /**
     * Обновление только username
     */
    public void updateUsernameCookie(HttpServletResponse response, String username) {
        setCookie(response, USERNAME_COOKIE, username, false);
    }

    /**
     * Обновление только accessToken (для обновления времени жизни)
     */
    public void updateTokenCookie(HttpServletResponse response, String accessToken) {
        setCookie(response, TOKEN_COOKIE, accessToken, false);
    }

    private void setCookie(HttpServletResponse response, String name, String value, boolean httpOnly) {
        Cookie cookie = new Cookie(name, value != null ? value : "");
        cookie.setHttpOnly(httpOnly);
        cookie.setPath("/");
        cookie.setMaxAge(value != null ? maxAge : 0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        setCookie(response, name, null, false);
    }
}