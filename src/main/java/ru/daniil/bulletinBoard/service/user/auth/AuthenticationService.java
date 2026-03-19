package ru.daniil.bulletinBoard.service.user.auth;

import jakarta.servlet.http.HttpServletResponse;
import ru.daniil.bulletinBoard.entity.request.auth.LoginRequest;
import ru.daniil.bulletinBoard.entity.request.auth.RegistrationRequest;
import ru.daniil.bulletinBoard.entity.response.auth.JwtResponse;
import ru.daniil.bulletinBoard.enums.AuthProvider;

public interface AuthenticationService {
    LoginRequest registerUserAndConvertToLoginRequest(RegistrationRequest request);

    JwtResponse exchangeCodeForToken(String code);

    JwtResponse authenticate(LoginRequest request, HttpServletResponse response);

    JwtResponse refreshToken(String refreshToken, HttpServletResponse response);

    void logout(String refreshToken, HttpServletResponse response);

    void register(JwtResponse data, AuthProvider provider);
}
