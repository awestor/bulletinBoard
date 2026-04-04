package ru.daniil.user.controller.auth;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import ru.daniil.core.request.auth.LoginRequest;
import ru.daniil.core.request.auth.RegistrationRequest;
import ru.daniil.core.response.MessageResponse;
import ru.daniil.core.response.auth.JwtResponse;
import ru.daniil.core.enums.AuthProvider;
import ru.daniil.user.service.auth.AuthenticationService;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Аутентификация", description = "API для регистрации, входа и управления токенами")
public class AuthApiController {

    private final AuthenticationService authenticationService;

    public AuthApiController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/token")
    @Hidden
    @Operation(summary = "Получение токена после входа в систему через IdP")
    public ResponseEntity<?> exchangeCode(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        String provider = payload.get("provider");
        JwtResponse jwtResponse = authenticationService.exchangeCodeForToken(code);

        try {
            System.err.println("Провайдер равен: " + provider);
            authenticationService.register(jwtResponse, AuthProvider.valueOf(provider));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Ошибка на внутренней стороне при обработке входа"));
        }

        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "Вход в систему")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        try {
            JwtResponse jwtResponse = authenticationService.authenticate(request, response);
            return ResponseEntity.ok(jwtResponse);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Ошибка при входе в систему"));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Регистрация")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegistrationRequest request,
            HttpServletResponse response) {
        try {
            LoginRequest loginRequest = authenticationService.registerUserAndConvertToLoginRequest(request);

            JwtResponse jwtResponse = authenticationService.authenticate(loginRequest, response);
            return ResponseEntity.ok(jwtResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Ошибка при регистрации: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновление токена")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = getRefreshTokenFromCookie(request);
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("Refresh token не найден"));
            }

            JwtResponse jwtResponse = authenticationService.refreshToken(refreshToken, response);
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Не удалось обновить токен"));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = getRefreshTokenFromCookie(request);
            authenticationService.logout(refreshToken, response);
            return ResponseEntity.ok(new MessageResponse("Успешный выход из системы"));
        } catch (Exception e) {
            authenticationService.logout(null, response);
            return ResponseEntity.ok(new MessageResponse("Выход выполнен"));
        }
    }

    @Deprecated
    @PostMapping("/registerByToken")
    @Hidden
    @Operation(summary = "Регистрация по токену")
    public ResponseEntity<?> registerByToken(
            @Valid @RequestBody JwtResponse data, String authProvider) {
        try {
            authenticationService.register(data, AuthProvider.valueOf(authProvider));
            return ResponseEntity.ok(new MessageResponse("Данные пользователя зарегистрированы"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Ошибка при регистрации: " + e.getMessage()));
        }
    }

    /**
     * Получение refresh токена из cookie
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "refresh_token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}