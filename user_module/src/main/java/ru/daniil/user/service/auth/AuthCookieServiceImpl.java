package ru.daniil.user.service.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.daniil.core.enums.CookieType;

@Service
public class AuthCookieServiceImpl implements AuthCookieService {

    @Value("${app.cookie.max-age:3600}")
    private int maxAge;

    /**
     * Установка cookie после успешного входа
     */
    public void setAuthCookies(HttpServletResponse response, String accessToken, String username) {
        setCookie(response, CookieType.ACCESS_TOKEN.toString(), accessToken, true);
        setCookie(response, CookieType.USERNAME.toString(), username.replaceAll("[\\s;,]", "_"), false);
    }

    /**
     * Удаление cookie при выходе
     */
    public void clearAuthCookies(HttpServletResponse response) {
        clearCookie(response, CookieType.ACCESS_TOKEN.toString());
        clearCookie(response, CookieType.USERNAME.toString());
    }

    /**
     * Обновление только username
     */
    public void updateUsernameCookie(HttpServletResponse response, String username) {
        setCookie(response, CookieType.USERNAME.toString(), username, false);
    }

    /**
     * Обновление только accessToken (для обновления времени жизни)
     */
    public void updateTokenCookie(HttpServletResponse response, String accessToken) {
        setCookie(response, CookieType.ACCESS_TOKEN.toString(), accessToken, false);
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