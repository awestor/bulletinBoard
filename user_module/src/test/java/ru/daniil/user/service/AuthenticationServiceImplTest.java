package ru.daniil.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.auth.LoginRequest;
import ru.daniil.core.request.auth.RegistrationRequest;
import ru.daniil.core.response.auth.JwtResponse;
import ru.daniil.core.enums.AuthProvider;
import ru.daniil.core.exceptions.UserBlockedExeption;
import ru.daniil.core.exceptions.UserNotFoundException;
import ru.daniil.user.service.auth.*;
import ru.daniil.user.service.user.UserService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthCookieService authCookieService;

    @Mock
    private HttpServletResponse response;

    private AuthenticationManager authenticationManager;
    private AuthenticationServiceImpl authenticationService;

    private RegistrationRequest registrationRequest;
    private User user;
    private LoginRequest loginRequest;
    private JwtResponse jwtResponse;

    @BeforeEach
    void setUp() throws Exception {
        authenticationManager = mock(AuthenticationManager.class);

        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        authenticationService = new AuthenticationServiceImpl(
                authenticationConfiguration,
                keycloakService,
                userDetailsService,
                userService,
                authCookieService
        );

        registrationRequest = RegistrationRequest.builder()
                .login("testuser")
                .email("test@test.com")
                .password("Test123!$%")
                .authProvider(AuthProvider.LOCAL)
                .build();

        user = new User("test@test.com", "testuser", "encodedPassword", AuthProvider.LOCAL);
        user.setId(1L);

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Test123!$%");

        jwtResponse = JwtResponse.builder()
                .accessToken("access.token.here")
                .expiresIn(3600L)
                .type("Bearer")
                .build();
    }

    @Test
    void authenticate_WithLocalUserAndKeycloakExists_ShouldAuthenticate() throws Exception {
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn((UserDetails) user);
        when(keycloakService.getAdminToken()).thenReturn("admin-token");
        when(keycloakService.findUserInKeycloak("testuser", "admin-token"))
                .thenReturn(Optional.of("keycloak-user-id"));
        when(keycloakService.authenticateAndGetTokens("testuser", "Test123!$%", response))
                .thenReturn(jwtResponse);

        JwtResponse result = authenticationService.authenticate(loginRequest, response);

        assertNotNull(result);
        assertEquals(jwtResponse, result);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(keycloakService, never()).createUserInKeycloak(any(), anyString(), any());
        verify(authCookieService).setAuthCookies(response, jwtResponse.getAccessToken(), "testuser");
    }

    @Test
    void authenticate_WithLocalUserAndKeycloakNotExists_ShouldCreateInKeycloak() throws Exception {
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn((UserDetails) user);
        when(keycloakService.getAdminToken()).thenReturn("admin-token");
        when(keycloakService.findUserInKeycloak("testuser", "admin-token"))
                .thenReturn(Optional.empty());
        when(keycloakService.authenticateAndGetTokens("testuser", "Test123!$%", response))
                .thenReturn(jwtResponse);

        JwtResponse result = authenticationService.authenticate(loginRequest, response);

        assertNotNull(result);
        assertEquals(jwtResponse, result);
        verify(keycloakService).createUserInKeycloak(user, "Test123!$%", response);
        verify(authCookieService).setAuthCookies(response, jwtResponse.getAccessToken(), "testuser");
    }

    @Test
    void authenticate_WithUserNotFoundLocally_ShouldTryKeycloakAuth() throws Exception {
        when(userDetailsService.loadUserByUsername("testuser"))
                .thenThrow(new UserNotFoundException("User not found"));
        when(keycloakService.authenticateAndGetTokens("testuser", "Test123!$%", response))
                .thenReturn(jwtResponse);

        JwtResponse result = authenticationService.authenticate(loginRequest, response);

        assertNotNull(result);
        assertEquals(jwtResponse, result);
        verify(authenticationManager, never()).authenticate(any());
        verify(keycloakService, never()).getAdminToken();
        verify(keycloakService, never()).findUserInKeycloak(anyString(), anyString());
        verify(authCookieService, never()).setAuthCookies(any(), anyString(), anyString());
    }

    @Test
    void authenticate_WithUserNotFoundAndKeycloakFails_ShouldThrowBadCredentials() throws Exception {
        when(userDetailsService.loadUserByUsername("testuser"))
                .thenThrow(new UserNotFoundException("User not found"));
        when(keycloakService.authenticateAndGetTokens("testuser", "Test123!$%", response))
                .thenThrow(new RuntimeException("Keycloak error"));

        assertThrows(BadCredentialsException.class,
                () -> authenticationService.authenticate(loginRequest, response));
    }

    @Test
    void authenticate_WithBlockedUser_ShouldThrowBadCredentials() throws Exception {
        when(userDetailsService.loadUserByUsername("testuser"))
                .thenThrow(new UserBlockedExeption("User is blocked"));

        assertThrows(BadCredentialsException.class,
                () -> authenticationService.authenticate(loginRequest, response));

        verify(authenticationManager, never()).authenticate(any());
        verify(keycloakService, never()).authenticateAndGetTokens(anyString(), anyString(), any());
    }

    @Test
    void authenticate_WithInvalidCredentials_ShouldThrowBadCredentials() throws Exception {
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn((UserDetails) user);
        doThrow(new BadCredentialsException("Invalid credentials"))
                .when(authenticationManager).authenticate(any());

        assertThrows(BadCredentialsException.class,
                () -> authenticationService.authenticate(loginRequest, response));

        verify(keycloakService, never()).getAdminToken();
        verify(keycloakService, never()).findUserInKeycloak(anyString(), anyString());
    }

    @Test
    void authenticate_WithKeycloakCreationError_ShouldThrowRuntimeException() throws Exception {
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn((UserDetails) user);
        when(keycloakService.getAdminToken()).thenReturn("admin-token");
        when(keycloakService.findUserInKeycloak("testuser", "admin-token"))
                .thenReturn(Optional.empty());
        doThrow(new RuntimeException("Keycloak creation error"))
                .when(keycloakService).createUserInKeycloak(user, "Test123!$%", response);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authenticationService.authenticate(loginRequest, response));

        assertTrue(exception.getMessage().contains("Ошибка при при создании пользователя в Keycloak"));
        verify(authCookieService, never()).setAuthCookies(any(), anyString(), anyString());
    }

    @Test
    void logout_ShouldCallKeycloakLogoutAndClearCookies() {
        String refreshToken = "refresh-token";
        doNothing().when(keycloakService).logout(refreshToken, response);
        doNothing().when(authCookieService).clearAuthCookies(response);

        authenticationService.logout(refreshToken, response);

        verify(keycloakService).logout(refreshToken, response);
        verify(authCookieService).clearAuthCookies(response);
    }

    @Test
    void registerUserAndConvertToLoginRequest_ShouldRegisterAndConvert() {
        when(userService.registerUser(registrationRequest)).thenReturn((UserDetails) user);

        LoginRequest result = authenticationService.registerUserAndConvertToLoginRequest(registrationRequest);

        assertNotNull(result);
        assertEquals(user.getLogin(), result.getUsername());
        assertEquals(registrationRequest.getPassword(), result.getPassword());
        verify(userService).registerUser(registrationRequest);
    }

    @Test
    void authByToken_ShouldReturnJwtResponse() {
        String code = "test-code";
        String validAccessToken = createTestJwtToken();
        JwtResponse validJwtResponse = JwtResponse.builder()
                .accessToken(validAccessToken)
                .expiresIn(3600L)
                .type("Bearer")
                .build();

        when(keycloakService.exchange(code)).thenReturn(validJwtResponse);
        when(userService.existsByEmail("test@test.com")).thenReturn(true); // Пользователь уже существует

        JwtResponse result = authenticationService.authByToken(code, AuthProvider.GOOGLE.toString(), response);

        assertNotNull(result);
        assertEquals(validJwtResponse, result);
        verify(keycloakService).exchange(code);
        verify(userService).existsByEmail("test@test.com");
        // Проверяем, что cookies установлены
        verify(authCookieService).setAuthCookies(response, validAccessToken, "testuser");
    }

    @Test
    void refreshToken_ShouldReturnJwtResponse() {
        String refreshToken = "refresh-token";
        when(keycloakService.refreshAccessToken(refreshToken, response)).thenReturn(jwtResponse);

        JwtResponse result = authenticationService.refreshToken(refreshToken, response);

        assertNotNull(result);
        assertEquals(jwtResponse, result);
        verify(keycloakService).refreshAccessToken(refreshToken, response);
    }

    @Test
    void refreshToken_WhenFails_ShouldThrowRuntimeException() {
        String refreshToken = "refresh-token";
        when(keycloakService.refreshAccessToken(refreshToken, response))
                .thenThrow(new RuntimeException("Refresh failed"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authenticationService.refreshToken(refreshToken, response));

        assertEquals("Не удалось обновить токен", exception.getMessage());
    }

    @Test
    void logout_ShouldCallKeycloakLogout() {
        String refreshToken = "refresh-token";
        doNothing().when(keycloakService).logout(refreshToken, response);

        authenticationService.logout(refreshToken, response);

        verify(keycloakService).logout(refreshToken, response);
    }

    @Test
    void authByToken_WithNewUser_ShouldRegisterAndSetCookies() {
        String code = "test-code";
        String accessToken = createTestJwtToken();
        JwtResponse jwtResponseWithToken = JwtResponse.builder()
                .accessToken(accessToken)
                .expiresIn(3600L)
                .type("Bearer")
                .build();

        when(keycloakService.exchange(code)).thenReturn(jwtResponseWithToken);
        when(userService.existsByEmail("test@test.com")).thenReturn(false);
        when(userService.existsByLogin("testuser")).thenReturn(false);
        when(userService.registerUserWithoutValidation(any(RegistrationRequest.class)))
                .thenReturn(user);

        JwtResponse result = authenticationService.authByToken(code, AuthProvider.GOOGLE.toString(), response);

        assertNotNull(result);
        assertEquals(jwtResponseWithToken, result);
        verify(keycloakService).exchange(code);
        verify(userService).existsByEmail("test@test.com");
        verify(userService).existsByLogin("testuser");
        verify(userService).registerUserWithoutValidation(any(RegistrationRequest.class));
        verify(authCookieService).setAuthCookies(response, accessToken, "testuser");
    }

    @Test
    void authByToken_WithExistingUser_ShouldSkipRegistrationButSetCookies() {
        String code = "test-code";
        String accessToken = createTestJwtToken();
        JwtResponse jwtResponseWithToken = JwtResponse.builder()
                .accessToken(accessToken)
                .expiresIn(3600L)
                .type("Bearer")
                .build();

        when(keycloakService.exchange(code)).thenReturn(jwtResponseWithToken);
        when(userService.existsByEmail("test@test.com")).thenReturn(true);

        JwtResponse result = authenticationService.authByToken(code, AuthProvider.GOOGLE.toString(), response);

        assertNotNull(result);
        assertEquals(jwtResponseWithToken, result);
        verify(keycloakService).exchange(code);
        verify(userService).existsByEmail("test@test.com");
        verify(userService, never()).existsByLogin(anyString());
        verify(userService, never()).registerUserWithoutValidation(any());
        verify(authCookieService).setAuthCookies(response, accessToken, "testuser");
    }

    @Test
    void authByToken_WithExistingLogin_ShouldGenerateUniqueLogin() {
        String code = "test-code";
        String accessToken = createTestJwtToken();
        JwtResponse jwtResponseWithToken = JwtResponse.builder()
                .accessToken(accessToken)
                .expiresIn(3600L)
                .type("Bearer")
                .build();

        when(keycloakService.exchange(code)).thenReturn(jwtResponseWithToken);
        when(userService.existsByEmail("test@test.com")).thenReturn(false);
        when(userService.existsByLogin("testuser")).thenReturn(true);
        when(userService.registerUserWithoutValidation(any(RegistrationRequest.class)))
                .thenReturn(user);

        JwtResponse result = authenticationService.authByToken(code, AuthProvider.GOOGLE.toString(), response);

        assertNotNull(result);
        assertEquals(jwtResponseWithToken, result);
        verify(keycloakService).exchange(code);
        verify(userService).existsByLogin("testuser");
        verify(userService).registerUserWithoutValidation(argThat(request ->
                request.getLogin() != null &&
                        !request.getLogin().equals("testuser") &&
                        request.getLogin().length() <= 16
        ));
        verify(authCookieService).setAuthCookies(response, accessToken, "testuser");
    }

    @Test
    void authByToken_WithInvalidToken_ShouldThrowRuntimeException() {
        String code = "test-code";
        String invalidToken = "invalid.token";
        JwtResponse jwtResponseWithInvalidToken = JwtResponse.builder()
                .accessToken(invalidToken)
                .expiresIn(3600L)
                .type("Bearer")
                .build();

        when(keycloakService.exchange(code)).thenReturn(jwtResponseWithInvalidToken);

        assertThrows(RuntimeException.class,
                () -> authenticationService.authByToken(code, AuthProvider.GOOGLE.toString(), response));

        verify(authCookieService, never()).setAuthCookies(any(), anyString(), anyString());
    }

    @Test
    void authByToken_WithRegistrationError_ShouldThrowBadCredentialsException() {
        String code = "test-code";
        String accessToken = createTestJwtToken();
        JwtResponse jwtResponseWithToken = JwtResponse.builder()
                .accessToken(accessToken)
                .expiresIn(3600L)
                .type("Bearer")
                .build();

        when(keycloakService.exchange(code)).thenReturn(jwtResponseWithToken);
        when(userService.existsByEmail("test@test.com")).thenReturn(false);
        when(userService.existsByLogin("testuser")).thenReturn(false);
        when(userService.registerUserWithoutValidation(any(RegistrationRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(BadCredentialsException.class,
                () -> authenticationService.authByToken(code, AuthProvider.GOOGLE.toString(), response));

        verify(authCookieService, never()).setAuthCookies(any(), anyString(), anyString());
    }

    private String createTestJwtToken() {
        try {
            String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());

            Map<String, Object> claims = new HashMap<>();
            claims.put("email", "test@test.com");
            claims.put("name", "testuser");
            claims.put("preferred_username", "testuser");
            claims.put("sub", "GOOGLE|123456789");

            ObjectMapper mapper = new ObjectMapper();
            String payloadJson = mapper.writeValueAsString(claims);
            String payload = Base64.getUrlEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().encodeToString("signature".getBytes());

            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test token", e);
        }
    }
}