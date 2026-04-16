package ru.daniil.user.service.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.enums.CookieType;
import ru.daniil.core.response.auth.JwtResponse;

import java.util.Map;
import java.util.Optional;

@Service
public class KeycloakServiceImpl implements KeycloakService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${keycloak.admin.username}")
    private String adminLogin;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    private static final int REFRESH_TOKEN_EXPIRY = 30 * 24 * 60 * 60; // 30 дней в секундах

    private static final Logger infoLogger = LoggerFactory.getLogger("INFO-LOGGER");

    /**
     * Аутентификация и получение токенов
     * Refresh token устанавливается в httpOnly cookie
     */
    @Override
    public JwtResponse authenticateAndGetTokens(
            String username, String rawPassword, HttpServletResponse response) {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                authServerUrl, realm);

        System.out.println("=== AUTHENTICATE TO KEYCLOAK ===");
        System.out.println("URL: " + tokenUrl);
        System.out.println("Username: " + username);
        System.out.println("Client ID: " + clientId);
        System.out.println("Secret length: " + (clientSecret != null ? clientSecret.length() : "null"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", username);
        body.add("password", rawPassword);

        try {
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> keycloakResponse = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            System.out.println("Response status: " + keycloakResponse.getStatusCode());

            if (keycloakResponse.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> tokenResponse = keycloakResponse.getBody();
                System.out.println("Access token получен: " + (tokenResponse.get("access_token") != null));

                String refreshToken = (String) tokenResponse.get("refresh_token");
                setRefreshTokenCookie(response, refreshToken);

                new JwtResponse();
                return JwtResponse.builder()
                        .accessToken((String) tokenResponse.get("access_token"))
                        .expiresIn(((Number) tokenResponse.get("expires_in")).longValue())
                        .build();
            }
        } catch (HttpClientErrorException e) {
            System.out.println("=== KEYCLOAK ERROR DETAILS ===");
            System.out.println("Status code: " + e.getStatusCode());
            System.out.println("Response body: " + e.getResponseBodyAsString());
            System.out.println("===============================");
            handleKeycloakError(e);
        }

        throw new RuntimeException("Failed to get tokens from Keycloak");
    }

    /**
     * Создание пользователя в Keycloak
     */
    @Override
    public void createUserInKeycloak(User user, String rawPassword, HttpServletResponse response) {
        try {
            String adminToken = getAdminToken();

            Optional<String> existingUserId = findUserInKeycloak(user.getUsername(), adminToken);

            if (existingUserId.isEmpty()) {
                createKeycloakUser(user, rawPassword, adminToken);
            }

            authenticateAndGetTokens(user.getUsername(), rawPassword, response);

        } catch (Exception e) {
            throw new RuntimeException("Error creating user in Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Обновление access токена с использованием refresh токена из cookie
     */
    @Override
    public JwtResponse refreshAccessToken(String refreshToken, HttpServletResponse response) {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                authServerUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        try {
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> keycloakResponse = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (keycloakResponse.getStatusCode() == HttpStatus.OK && keycloakResponse.getBody() != null) {
                Map<String, Object> tokenResponse = keycloakResponse.getBody();

                String newRefreshToken = (String) tokenResponse.get("refresh_token");
                if (newRefreshToken != null) {
                    setRefreshTokenCookie(response, newRefreshToken);
                }

                new JwtResponse();
                return JwtResponse.builder()
                        .accessToken((String) tokenResponse.get("access_token"))
                        .expiresIn(((Number) tokenResponse.get("expires_in")).longValue())
                        .build();
            }
        } catch (HttpClientErrorException e) {
            handleKeycloakError(e);
        }

        throw new RuntimeException("Failed to refresh token");
    }

    /**
     * Удаление refresh токена из cookie (при logout)
     */
    @Override
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * Logout в Keycloak
     */
    @Override
    public void logout(String refreshToken, HttpServletResponse response) {
        String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout",
                authServerUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(logoutUrl, entity, String.class);
        } catch (Exception e) {
            System.err.println("Error during Keycloak logout: " + e.getMessage());
        } finally {
            clearRefreshTokenCookie(response);
        }
    }

    /**
     * Получение admin токена
     */
    @Override
    public String getAdminToken() {
        String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", authServerUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", "admin-cli");
        body.add("username", adminLogin);
        body.add("password", adminPassword);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            System.err.println("Не удалось получить admin token");
            throw new RuntimeException("Не удалось получить admin token", e);
        }

        System.err.println("Не удалось получить admin token");
        throw new RuntimeException("Не удалось получить admin token");
    }


    /**
     * Поиск пользователя в Keycloak
     */
    @Override
    public Optional<String> findUserInKeycloak(String username, String adminToken) {
        String findUserUrl = String.format("%s/admin/realms/%s/users?username=%s",
                authServerUrl, realm, username);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map[]> response = restTemplate.exchange(
                    findUserUrl,
                    HttpMethod.GET,
                    entity,
                    Map[].class
            );

            if (response.getBody() != null && response.getBody().length > 0) {
                return Optional.of((String) response.getBody()[0].get("id"));
            }
        } catch (Exception e) {
            System.err.println("Ошибка при поиске пользователя: " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Преобразование ответа от keyCloak после входа с помощью внешнего провайдера
     */
    @Override
    public JwtResponse exchange(String code, HttpServletResponse response){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", "http://localhost:8080/login");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        String tokenUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        try {
            ResponseEntity<Map> keycloakResponse = restTemplate.postForEntity(tokenUrl, request, Map.class);

            Map<String, Object> tokenResponse = keycloakResponse.getBody();

            assert tokenResponse != null;
            setRefreshTokenCookie(response, (String) tokenResponse.get("refresh_token"));

            return JwtResponse.builder()
                    .accessToken((String) tokenResponse.get("access_token"))
                    .expiresIn(((Number) tokenResponse.get("expires_in")).longValue())
                    .type((String) tokenResponse.getOrDefault("token_type", "Bearer"))
                    .build();

        } catch (HttpClientErrorException e) {
            System.err.println("=== HTTP ERROR ===");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Response body: " + e.getResponseBodyAsString());
            System.err.println("=================");
            throw new RuntimeException("Keycloak token exchange failed: " + e.getResponseBodyAsString(), e);
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Установка refresh токена в httpOnly cookie
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(CookieType.REFRESH_TOKEN.toString(), refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(REFRESH_TOKEN_EXPIRY);
        // cookie.setDomain("your-domain.com");
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    /**
     * Создание пользователя в Keycloak
     */
    private void createKeycloakUser(User user, String rawPassword, String adminToken) {
        String createUserUrl = String.format("%s/admin/realms/%s/users", authServerUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> userPayload = Map.of(
                "username", user.getUsername(),
                "email", user.getEmail(),
                "emailVerified", true,
                "enabled", true,
                "firstName", "---",
                "lastName", "---",
                "attributes", Map.of("localUserId", user.getId().toString())
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(userPayload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(createUserUrl, entity, String.class);

        if (response.getHeaders().getLocation() != null) {
            String location = response.getHeaders().getLocation().getPath();
            String userId = location.substring(location.lastIndexOf("/") + 1);

            String resetPasswordUrl = String.format("%s/admin/realms/%s/users/%s/reset-password",
                    authServerUrl, realm, userId);

            Map<String, Object> credential = Map.of(
                    "type", "password",
                    "value", rawPassword,
                    "temporary", false
            );

            HttpEntity<Map<String, Object>> passwordEntity = new HttpEntity<>(credential, headers);
            restTemplate.put(resetPasswordUrl, passwordEntity);

            System.err.println("Пользователь создан и пароль установлен");
            return;
        }

        throw new RuntimeException("Не удалось получить ID созданного пользователя");
    }

    /**
     * Обработка ошибок Keycloak
     */
    private void handleKeycloakError(HttpClientErrorException e) {
        String errorMessage = "Ошибка Keycloak: ";
        if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            errorMessage += "Неверные учетные данные";
        } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
            errorMessage += "Неверный запрос: " + e.getResponseBodyAsString();
        } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
            errorMessage += "Доступ запрещен";
        } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            errorMessage += "Пользователь не найден";
        } else {
            errorMessage += e.getResponseBodyAsString();
        }
        throw new RuntimeException(errorMessage, e);
    }
}