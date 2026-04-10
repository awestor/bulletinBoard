package ru.daniil.user.config;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
public class JwtCookieFilter extends OncePerRequestFilter {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/auth/**",
            "/login",
            "/register",
            "/css/**",
            "/js/**"
    );

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

        // Проверка, нет ли уже заголовка Authorization
        if (request.getHeader("Authorization") == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("ACCESS_TOKEN".equals(cookie.getName())) {
                        HttpServletRequest wrappedRequest = getHttpServletRequest(request, cookie);
                        filterChain.doFilter(wrappedRequest, response);
                        return;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private static @NonNull HttpServletRequest getHttpServletRequest(HttpServletRequest request, Cookie cookie) {
        String token = cookie.getValue();
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("Authorization".equals(name)) {
                    return "Bearer " + token;
                }
                return super.getHeader(name);
            }
        };
    }
}
