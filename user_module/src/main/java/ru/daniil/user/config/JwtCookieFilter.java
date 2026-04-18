package ru.daniil.user.config;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.daniil.core.enums.CookieType;
import ru.daniil.core.response.auth.JwtResponse;
import ru.daniil.user.service.auth.AuthenticationService;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtCookieFilter extends OncePerRequestFilter {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtDecoder jwtDecoder;

    private final AuthenticationService authenticationService;

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/auth/**",
            "/login",
            "/register",
            "/css/**",
            "/js/**",
            "/error"
    );

    public JwtCookieFilter(JwtDecoder jwtDecoder,
                           @Lazy AuthenticationService authenticationService) {
        this.jwtDecoder = jwtDecoder;
        this.authenticationService = authenticationService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();

        return PUBLIC_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String accessToken = getCookieValue(request, CookieType.ACCESS_TOKEN.toString());
        String refreshToken = getCookieValue(request, CookieType.REFRESH_TOKEN.toString());

        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!isTokenExpired(accessToken)) {
            filterChain.doFilter(wrapWithAuthorizationHeader(request, accessToken), response);
            return;
        }

        if (refreshToken == null) {
            sendUnauthorized(response, "Токен для обновления не найден");
            return;
        }

        try {
            JwtResponse jwtResponse = authenticationService.refreshToken(refreshToken, response);

            filterChain.doFilter(
                    wrapWithAuthorizationHeader(request, jwtResponse.getAccessToken()),
                    response
            );

        } catch (Exception e) {
            authenticationService.logout(refreshToken, response);
            sendUnauthorized(response, "Сессия уже была прекращена войдите заново");
        }
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean isTokenExpired(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            Instant expiresAt = jwt.getExpiresAt();
            return expiresAt != null && expiresAt.isBefore(Instant.now());
        } catch (JwtException e) {
            return true;
        }
    }

    private HttpServletRequest wrapWithAuthorizationHeader(HttpServletRequest request, String accessToken) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("Authorization".equals(name)) {
                    return "Bearer " + accessToken;
                }
                return super.getHeader(name);
            }
        };
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\":\"%s\"}", message));
    }
}