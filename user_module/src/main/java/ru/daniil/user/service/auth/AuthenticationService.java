package ru.daniil.user.service.auth;

import jakarta.servlet.http.HttpServletResponse;
import ru.daniil.core.request.auth.LoginRequest;
import ru.daniil.core.request.auth.RegistrationRequest;
import ru.daniil.core.response.auth.JwtResponse;

public interface AuthenticationService {
    LoginRequest registerUserAndConvertToLoginRequest(RegistrationRequest request);

    JwtResponse authByToken(String code, String provider, HttpServletResponse response);

    JwtResponse authenticate(LoginRequest request, HttpServletResponse response);

    JwtResponse refreshToken(String refreshToken, HttpServletResponse response);

    void logout(String refreshToken, HttpServletResponse response);
}
