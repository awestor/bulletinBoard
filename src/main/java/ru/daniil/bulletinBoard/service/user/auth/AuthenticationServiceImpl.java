package ru.daniil.bulletinBoard.service.user.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import exceptions.UserBlockedExeption;
import exceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.entity.request.auth.LoginRequest;
import ru.daniil.bulletinBoard.entity.request.auth.RegistrationRequest;
import ru.daniil.bulletinBoard.entity.response.auth.JwtResponse;
import ru.daniil.bulletinBoard.enums.AuthProvider;
import ru.daniil.bulletinBoard.service.user.UserService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final KeycloakService keycloakService;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserService userService;

    public AuthenticationServiceImpl(
            AuthenticationConfiguration authenticationConfiguration,
            KeycloakService keycloakService,
            UserDetailsServiceImpl userDetailsService, UserService userService) {
        this.authenticationManager = authenticationConfiguration.getAuthenticationManager();
        this.keycloakService = keycloakService;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @Override
    public LoginRequest registerUserAndConvertToLoginRequest(RegistrationRequest request) {
        User user = (User) userService.registerUser(request);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(user.getLogin());
        loginRequest.setPassword(request.getPassword());
        return loginRequest;
    }

    @Override
    @Transactional
    public JwtResponse exchangeCodeForToken(String code) {
        return keycloakService.exchange(code);
    }

    @Override
    @Transactional
    public JwtResponse authenticate(LoginRequest request, HttpServletResponse response) {
        try {
            User user;
            try {
                user = (User) userDetailsService.loadUserByUsername(request.getUsername());
            } catch (UserNotFoundException e){
                try {
                    return keycloakService.authenticateAndGetTokens(
                            request.getUsername(),
                            request.getPassword(),
                            response
                    );
                } catch (Exception ex) {
                    throw new BadCredentialsException(e.getMessage());
                }
            } catch (UserBlockedExeption e){
                throw new BadCredentialsException(e.getMessage());
            }
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
            try {
                Optional<String> keycloakUserId = keycloakService.findUserInKeycloak(
                        user.getUsername(),
                        keycloakService.getAdminToken()
                );

                if (keycloakUserId.isEmpty()) {
                    try {
                        keycloakService.createUserInKeycloak(user, request.getPassword(), response);
                    } catch (Exception ex){
                        throw new RuntimeException("Ошибка при при создании пользователя в Keycloak", ex);
                    }
                }

                return keycloakService.authenticateAndGetTokens(
                        user.getUsername(), request.getPassword(), response);
            } catch (Exception e) {
                System.err.println("Keycloak error: " + e.getMessage());
                throw new RuntimeException("Ошибка при получении токенов от Keycloak:" + e.getMessage());

            }
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    @Override
    public JwtResponse refreshToken(String refreshToken, HttpServletResponse response) {
        try {
            return keycloakService.refreshAccessToken(refreshToken, response);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось обновить токен", e);
        }
    }

    @Override
    public void logout(String refreshToken, HttpServletResponse response) {
        keycloakService.logout(refreshToken, response);
    }

    @Override
    public void register(JwtResponse data, AuthProvider provider) {
        String accessToken = data.getAccessToken();
        Map<String, Object> claims = decodeJwtToken(accessToken);

        if(userService.existsByEmail((String) claims.get("email"))){
            return;
        }

        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setLogin((String) claims.get("name"));
        registrationRequest.setEmail((String) claims.get("email"));
        registrationRequest.setAuthProvider(provider);
        registrationRequest.setPassword(null);

        userService.registerUserWithoutValidation(registrationRequest);
    }

    /**
     * Декодирует JWT токен для извлечения данных
     */
    private Map<String, Object> decodeJwtToken(String token) {
        try {
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) {
                throw new IllegalArgumentException("Невалидный JWT токен");
            }

            String payload = new String(Base64.getUrlDecoder().decode(chunks[1]), StandardCharsets.UTF_8);

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(payload, new TypeReference<>() {
            });

        } catch (Exception e) {
            throw new RuntimeException("Не удалось декодировать JWT токен", e);
        }
    }
}