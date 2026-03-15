package ru.daniil.bulletinBoard.controller.auth;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.daniil.bulletinBoard.entity.base.user.RefreshToken;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.entity.request.auth.LoginRequest;
import ru.daniil.bulletinBoard.entity.request.auth.RegistrationRequest;
import ru.daniil.bulletinBoard.entity.response.auth.JwtResponse;
import ru.daniil.bulletinBoard.entity.response.auth.RefreshTokenRequest;
import ru.daniil.bulletinBoard.service.user.auth.JwtService;
import ru.daniil.bulletinBoard.service.user.UserService;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    AuthApiController(UserService userService,
                      UserDetailsService userDetailsService,
                      PasswordEncoder passwordEncoder,
                      JwtService jwtService){
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request) {
        try{
            Optional<User> user = userService.getByLogin(request.getUsername());
            if (user.isPresent()) {
                if (!passwordEncoder.matches(request.getPassword(), user.get().getPassword())) {
                    return ResponseEntity.badRequest().build();
                }

                String accessToken = jwtService.generateToken(user.get());
                RefreshToken refreshToken = jwtService.generateRefreshToken(user.get());

                return ResponseEntity.ok(JwtResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken.getToken())
                        .expiresIn(1200L) // 20 минут в секундах
                        .build());
            }

            return ResponseEntity.badRequest().build();
        }
        catch (Exception e){
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationRequest registrationRequest,
                                          BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("Ошибка валидации");
        }

        try {
            UserDetails user = userService.registerUser(registrationRequest);
            String accessToken = jwtService.generateToken(user);
            RefreshToken refreshToken = jwtService.generateRefreshToken((User) user);

            return ResponseEntity.ok(JwtResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .expiresIn(1200L)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при создании: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String newAccessToken = jwtService.refreshAccessToken(request.getRefreshToken());

            return ResponseEntity.ok(JwtResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(request.getRefreshToken()) // возвращаем тот же refresh токен
                    .expiresIn(1200L)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenRequest request) {
        try {
            // Здесь будет логика для удаления действия refresh токена и выхода пользователя
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}