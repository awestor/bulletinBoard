package ru.daniil.user.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.enums.CookieType;
import ru.daniil.core.response.auth.JwtResponse;
import ru.daniil.user.service.auth.KeycloakServiceImpl;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HttpServletResponse response;

    private KeycloakServiceImpl keycloakService;

    private User user;
    private Map<String, Object> tokenResponse;
    private Map<String, Object> adminTokenResponse;

    private static final String AUTH_SERVER_URL = "http://localhost:9090";
    private static final String REALM = "bulletin-board";
    private static final String CLIENT_ID = "bulletin-board-client";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "admin-pass";

    @BeforeEach
    void setUp() {
        keycloakService = new KeycloakServiceImpl();
        ReflectionTestUtils.setField(keycloakService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(keycloakService, "authServerUrl", AUTH_SERVER_URL);
        ReflectionTestUtils.setField(keycloakService, "realm", REALM);
        ReflectionTestUtils.setField(keycloakService, "clientId", CLIENT_ID);
        ReflectionTestUtils.setField(keycloakService, "clientSecret", CLIENT_SECRET);
        ReflectionTestUtils.setField(keycloakService, "adminLogin", ADMIN_LOGIN);
        ReflectionTestUtils.setField(keycloakService, "adminPassword", ADMIN_PASSWORD);

        user = new User("test@test.com", "testuser", "password", null);
        user.setId(1L);

        tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "access-token-123");
        tokenResponse.put("refresh_token", "refresh-token-123");
        tokenResponse.put("expires_in", 300);
        tokenResponse.put("token_type", "Bearer");

        adminTokenResponse = new HashMap<>();
        adminTokenResponse.put("access_token", "admin-token-123");
        adminTokenResponse.put("expires_in", 3600);
        adminTokenResponse.put("token_type", "Bearer");
    }

    @Test
    void authenticateAndGetTokens_ShouldReturnJwtResponseAndSetCookie() {
        // Arrange
        String tokenUrl = AUTH_SERVER_URL + "/realms/" + REALM + "/protocol/openid-connect/token";
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Act
        JwtResponse result = keycloakService.authenticateAndGetTokens("testuser", "password", response);

        // Assert
        assertNotNull(result);
        assertEquals("access-token-123", result.getAccessToken());
        assertEquals(300L, result.getExpiresIn());

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());

        Cookie cookie = cookieCaptor.getValue();
        assertEquals(CookieType.REFRESH_TOKEN.toString(), cookie.getName());
        assertEquals("refresh-token-123", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals("/", cookie.getPath());
        assertEquals(60 * 60, cookie.getMaxAge());
        assertEquals("Strict", cookie.getAttribute("SameSite"));
    }

    @Test
    void authenticateAndGetTokens_WithHttpClientError_ShouldThrowRuntimeException() {
        // Arrange
        String tokenUrl = AUTH_SERVER_URL + "/realms/" + REALM + "/protocol/openid-connect/token";
        HttpClientErrorException.Unauthorized httpError = mock(HttpClientErrorException.Unauthorized.class);

        when(httpError.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);
        when(httpError.getResponseBodyAsString()).thenReturn("");
        when(restTemplate.exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(httpError);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> keycloakService.authenticateAndGetTokens("testuser", "wrong-password", response));

        assertTrue(exception.getMessage().contains("Ошибка Keycloak: Неверные учетные данные"));
    }

    @Test
    void authenticateAndGetTokens_WithBadRequest_ShouldThrowRuntimeException() {
        // Arrange
        String tokenUrl = AUTH_SERVER_URL + "/realms/" + REALM + "/protocol/openid-connect/token";
        HttpClientErrorException.BadRequest httpError = mock(HttpClientErrorException.BadRequest.class);

        when(httpError.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(httpError.getResponseBodyAsString()).thenReturn("Invalid grant");
        when(restTemplate.exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(httpError);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> keycloakService.authenticateAndGetTokens("testuser", "password", response));

        assertTrue(exception.getMessage().contains("Ошибка Keycloak: Неверный запрос"));
    }

    @Test
    void createUserInKeycloak_WhenUserNotExists_ShouldCreateUser() {
        // Arrange
        String adminToken = "admin-token-123";
        String findUserUrl = AUTH_SERVER_URL + "/admin/realms/" + REALM + "/users?username=testuser";

        // Мокаем получение admin токена
        String tokenUrl = AUTH_SERVER_URL + "/realms/master/protocol/openid-connect/token";
        ResponseEntity<Map> tokenResponseEntity = new ResponseEntity<>(adminTokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(tokenResponseEntity);

        // Мокаем поиск пользователя (не найден)
        ResponseEntity<Map[]> emptyResponse = new ResponseEntity<>(new Map[0], HttpStatus.OK);
        when(restTemplate.exchange(
                eq(findUserUrl),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map[].class)
        )).thenReturn(emptyResponse);

        // Мокаем создание пользователя
        String createUserUrl = AUTH_SERVER_URL + "/admin/realms/" + REALM + "/users";
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(createUserUrl + "/123"));
        ResponseEntity<String> createResponse = new ResponseEntity<>(headers, HttpStatus.CREATED);

        when(restTemplate.postForEntity(
                eq(createUserUrl),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(createResponse);

        // Мокаем установку пароля
        doNothing().when(restTemplate).put(contains("/reset-password"), any(HttpEntity.class));

        // Мокаем успешную аутентификацию после создания
        String authUrl = AUTH_SERVER_URL + "/realms/" + REALM + "/protocol/openid-connect/token";
        ResponseEntity<Map> authResponse = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(authUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(authResponse);

        // Act
        keycloakService.createUserInKeycloak(user, "password", response);

        // Assert
        verify(restTemplate).put(contains("/reset-password"), any(HttpEntity.class));
        // Проверяем точное количество вызовов exchange
        verify(restTemplate, times(3)).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), any(Class.class));
    }

    @Test
    void createUserInKeycloak_WhenUserExists_ShouldSkipCreation() {
        // Arrange
        String adminToken = "admin-token-123";
        String findUserUrl = AUTH_SERVER_URL + "/admin/realms/" + REALM + "/users?username=testuser";

        // Мокаем получение admin токена
        String tokenUrl = AUTH_SERVER_URL + "/realms/master/protocol/openid-connect/token";
        ResponseEntity<Map> tokenResponseEntity = new ResponseEntity<>(adminTokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(tokenResponseEntity);

        // Мокаем поиск пользователя (найден)
        Map<String, Object> existingUser = new HashMap<>();
        existingUser.put("id", "123");
        Map[] usersArray = new Map[]{existingUser};
        ResponseEntity<Map[]> findResponse = new ResponseEntity<>(usersArray, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(findUserUrl),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map[].class)
        )).thenReturn(findResponse);

        // Мокаем успешную аутентификацию
        String authUrl = AUTH_SERVER_URL + "/realms/" + REALM + "/protocol/openid-connect/token";
        ResponseEntity<Map> authResponse = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(authUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(authResponse);

        // Act
        keycloakService.createUserInKeycloak(user, "password", response);

        // Assert
        verify(restTemplate, never()).postForEntity(
                contains("/admin/realms/" + REALM + "/users"),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(restTemplate, never()).put(anyString(), any(HttpEntity.class));
        // Проверяем количество вызовов exchange (2: getAdminToken + findUser + authenticate)
        verify(restTemplate, times(3)).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
    }

    @Test
    void refreshAccessToken_ShouldReturnNewTokens() {
        // Arrange
        String refreshToken = "old-refresh-token";
        String tokenUrl = AUTH_SERVER_URL + "/realms/" + REALM + "/protocol/openid-connect/token";

        Map<String, Object> newTokens = new HashMap<>();
        newTokens.put("access_token", "new-access-token");
        newTokens.put("refresh_token", "new-refresh-token");
        newTokens.put("expires_in", 300);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(newTokens, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Act
        JwtResponse result = keycloakService.refreshAccessToken(refreshToken, response);

        // Assert
        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
        assertEquals(300L, result.getExpiresIn());

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());

        Cookie cookie = cookieCaptor.getValue();
        assertEquals(CookieType.REFRESH_TOKEN.toString(), cookie.getName());
        assertEquals("new-refresh-token", cookie.getValue());
    }

    @Test
    void refreshAccessToken_WhenNoNewRefreshToken_ShouldNotUpdateCookie() {
        // Arrange
        String refreshToken = "old-refresh-token";
        String tokenUrl = AUTH_SERVER_URL + "/realms/" + REALM + "/protocol/openid-connect/token";

        Map<String, Object> newTokens = new HashMap<>();
        newTokens.put("access_token", "new-access-token");
        newTokens.put("expires_in", 300);
        // refresh_token отсутствует

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(newTokens, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Act
        JwtResponse result = keycloakService.refreshAccessToken(refreshToken, response);

        // Assert
        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
        verify(response, never()).addCookie(any(Cookie.class));
    }

    @Test
    void logout_ShouldClearCookieAndCallKeycloak() {
        // Arrange
        String refreshToken = "refresh-token";
        String logoutUrl = AUTH_SERVER_URL + "/realms/" + REALM + "/protocol/openid-connect/logout";

        when(restTemplate.postForEntity(
                eq(logoutUrl),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        // Act
        keycloakService.logout(refreshToken, response);

        // Assert
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());

        Cookie cookie = cookieCaptor.getValue();
        assertEquals(CookieType.REFRESH_TOKEN.toString(), cookie.getName());
        assertEquals(0, cookie.getMaxAge());
        assertNull(cookie.getValue());
    }

    @Test
    void logout_WhenException_ShouldStillClearCookie() {
        // Arrange
        String refreshToken = "refresh-token";
        String logoutUrl = AUTH_SERVER_URL + "/realms/" + REALM + "/protocol/openid-connect/logout";

        when(restTemplate.postForEntity(
                eq(logoutUrl),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Logout failed"));

        // Act
        keycloakService.logout(refreshToken, response);

        // Assert
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());

        Cookie cookie = cookieCaptor.getValue();
        assertEquals(CookieType.REFRESH_TOKEN.toString(), cookie.getName());
        assertEquals(0, cookie.getMaxAge());
        assertNull(cookie.getValue());
    }

    @Test
    void getAdminToken_ShouldReturnToken() {
        // Arrange
        String tokenUrl = AUTH_SERVER_URL + "/realms/master/protocol/openid-connect/token";
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(adminTokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Act
        String result = keycloakService.getAdminToken();

        // Assert
        assertEquals("admin-token-123", result);
    }

    @Test
    void getAdminToken_WhenResponseHasNoToken_ShouldThrowRuntimeException() {
        // Arrange
        String tokenUrl = AUTH_SERVER_URL + "/realms/master/protocol/openid-connect/token";
        Map<String, Object> invalidResponse = new HashMap<>();
        // no access_token
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(invalidResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> keycloakService.getAdminToken());
    }

    @Test
    void getAdminToken_WhenFails_ShouldThrowRuntimeException() {
        // Arrange
        String tokenUrl = AUTH_SERVER_URL + "/realms/master/protocol/openid-connect/token";
        HttpClientErrorException.Unauthorized httpError = mock(HttpClientErrorException.Unauthorized.class);

        when(restTemplate.exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(httpError);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> keycloakService.getAdminToken());
    }

    @Test
    void findUserInKeycloak_WhenUserExists_ShouldReturnUserId() {
        // Arrange
        String adminToken = "admin-token";
        String findUserUrl = AUTH_SERVER_URL + "/admin/realms/" + REALM + "/users?username=testuser";

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", "user-123");
        userData.put("username", "testuser");
        userData.put("email", "test@test.com");
        Map[] usersArray = new Map[]{userData};

        ResponseEntity<Map[]> responseEntity = new ResponseEntity<>(usersArray, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(findUserUrl),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map[].class)
        )).thenReturn(responseEntity);

        // Act
        Optional<String> result = keycloakService.findUserInKeycloak("testuser", adminToken);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("user-123", result.get());
    }

    @Test
    void findUserInKeycloak_WhenUserNotExists_ShouldReturnEmpty() {
        // Arrange
        String adminToken = "admin-token";
        String findUserUrl = AUTH_SERVER_URL + "/admin/realms/" + REALM + "/users?username=testuser";

        ResponseEntity<Map[]> responseEntity = new ResponseEntity<>(new Map[0], HttpStatus.OK);

        when(restTemplate.exchange(
                eq(findUserUrl),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map[].class)
        )).thenReturn(responseEntity);

        // Act
        Optional<String> result = keycloakService.findUserInKeycloak("testuser", adminToken);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void findUserInKeycloak_WhenException_ShouldReturnEmpty() {
        // Arrange
        String adminToken = "admin-token";
        String findUserUrl = AUTH_SERVER_URL + "/admin/realms/" + REALM + "/users?username=testuser";

        when(restTemplate.exchange(
                eq(findUserUrl),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map[].class)
        )).thenThrow(new RuntimeException("Connection error"));

        // Act
        Optional<String> result = keycloakService.findUserInKeycloak("testuser", adminToken);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void exchange_ShouldReturnJwtResponse() {
        String code = "auth-code";
        String tokenUrl = AUTH_SERVER_URL + "/realms/" + REALM + "/protocol/openid-connect/token";

        Map<String, Object> exchangeResponse = new HashMap<>();
        exchangeResponse.put("access_token", "exchanged-token");
        exchangeResponse.put("expires_in", 3600);
        exchangeResponse.put("token_type", "Bearer");

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(exchangeResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(tokenUrl), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        JwtResponse result = keycloakService.exchange(code, response);

        assertNotNull(result);
        assertEquals("exchanged-token", result.getAccessToken());
        assertEquals(3600L, result.getExpiresIn());
        assertEquals("Bearer", result.getType());
    }

    @Test
    void exchange_WithNullTokenType_ShouldUseDefaultBearer() {
        String code = "auth-code";
        String tokenUrl = AUTH_SERVER_URL + "/realms/" + REALM + "/protocol/openid-connect/token";

        Map<String, Object> exchangeResponse = new HashMap<>();
        exchangeResponse.put("access_token", "exchanged-token");
        exchangeResponse.put("expires_in", 3600);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(exchangeResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(tokenUrl), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        JwtResponse result = keycloakService.exchange(code, response);

        assertEquals("Bearer", result.getType());
    }
}