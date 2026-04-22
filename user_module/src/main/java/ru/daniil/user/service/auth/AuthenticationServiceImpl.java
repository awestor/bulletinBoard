package ru.daniil.user.service.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.enums.AuthProvider;
import ru.daniil.core.exceptions.UserBlockedException;
import ru.daniil.core.exceptions.UserNotFoundException;
import ru.daniil.core.request.auth.LoginRequest;
import ru.daniil.core.request.auth.RegistrationRequest;
import ru.daniil.core.response.auth.JwtResponse;
import ru.daniil.user.service.user.UserService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final KeycloakService keycloakService;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserService userService;
    private final AuthCookieService authCookieService;
    private static final Logger infoLogger = LoggerFactory.getLogger("INFO-LOGGER");

    public AuthenticationServiceImpl(
            KeycloakService keycloakService,
            UserDetailsServiceImpl userDetailsService,
            UserService userService,
            AuthCookieService authCookieService) {
        this.keycloakService = keycloakService;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.authCookieService = authCookieService;
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
    public JwtResponse authByToken(String code, String provider, HttpServletResponse response) {
        infoLogger.info("Пользователь пришёл с токеном и хочет обменять его на jwtResponse");
        JwtResponse jwtResponse = keycloakService.exchange(code, response);

        register(jwtResponse, AuthProvider.valueOf(provider.toUpperCase()), response);
        infoLogger.info("jwtResponse пользователя был получен успешно");
        return jwtResponse;
    }

    @Override
    public JwtResponse authenticate(LoginRequest request, HttpServletResponse response) {
        try {
            User user = null;
            try {
                //авторизация через локальную БД
                user = (User) userDetailsService.loadUserByUsername(request.getUsername());
            } catch (UserNotFoundException e){
                //ничего не делать
            } catch (UserBlockedException ex){
                throw new BadCredentialsException(ex.getMessage());
            }

            if (user != null) {
                //если в локальную БД добавили, а в keycloak - нет
                Optional<String> keycloakUserId = keycloakService.findUserInKeycloak(
                        user.getUsername(),
                        keycloakService.getAdminToken()
                );

                if (keycloakUserId.isEmpty()) {
                    keycloakService.createUserInKeycloak(user, request.getPassword(), response);
                }
            }

            JwtResponse jwtResponse;
            try {
                //авторизация через keycloak
                jwtResponse = keycloakService.authenticateAndGetTokens(
                        request.getUsername(),
                        request.getPassword(),
                        response
                );
            } catch (Exception e) {
                throw new BadCredentialsException("Не валидный password или username");
            }

            if (jwtResponse != null && user == null) {
                //а это наоборот, на случай если в БД нет, но в keycloak - есть
                register(jwtResponse, AuthProvider.KEYCLOAK, response);
                return jwtResponse;
            }

            if (jwtResponse != null) {
                authCookieService.setAuthCookies(response, jwtResponse.getAccessToken(), request.getUsername());
            }
            return jwtResponse;

        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    @Override
    public JwtResponse refreshToken(String refreshToken, HttpServletResponse response) {
        try {
            JwtResponse jwtResponse = keycloakService.refreshAccessToken(refreshToken, response);
            authCookieService.updateTokenCookie(response, jwtResponse.getAccessToken());
            return jwtResponse;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось обновить токен", e);
        }
    }

    @Override
    public void logout(String refreshToken, HttpServletResponse response) {
        keycloakService.logout(refreshToken, response);
        authCookieService.clearAuthCookies(response);
    }

    @Transactional
    private void register(JwtResponse data, AuthProvider provider, HttpServletResponse response) {
        String accessToken = data.getAccessToken();
        Map<String, Object> claims = decodeJwtToken(accessToken);

        if(userService.existsByEmail((String) claims.get("email"))){
            infoLogger.info("Входящий пользователь найден в БД");
            String username = (String) claims.get("name");
            authCookieService.setAuthCookies(response, accessToken, username);
            return;
        }

        RegistrationRequest registrationRequest = new RegistrationRequest();

        String rawUsername = (String) claims.get("name");
        String username = rawUsername.replaceAll("[\\s;,]", "");
        if(userService.existsByLogin(username)){
            registrationRequest.setLogin(
                    UUID.randomUUID().toString().replace("-", "").substring(0, 16)
            );
        }
        else {
            registrationRequest.setLogin(username);
        }

        registrationRequest.setEmail((String) claims.get("email"));
        registrationRequest.setAuthProvider(provider);
        registrationRequest.setPassword(null);

        try {
            userService.registerUserWithoutValidation(registrationRequest);
            //В cookie указывается именно имя аккаунта внешнего провайдера, а не его фактический ник,
            // ведь его дубль в БД не столь важен для отображения и нужен только для осуществления связей в БД
            infoLogger.info("Регистрация прошла успешно");
        }
        catch(Exception ex){
            infoLogger.info("Регистрация прошла не удачно");
            throw new BadCredentialsException("При создании пользователя с переданными данными возникла ошибка");
        }
        authCookieService.setAuthCookies(response, data.getAccessToken(), username);
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