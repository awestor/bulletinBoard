package ru.daniil.bulletinBoard.controller.auth;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.entity.request.auth.LoginRequest;
import ru.daniil.bulletinBoard.entity.request.auth.RegistrationRequest;
import ru.daniil.bulletinBoard.entity.response.jwt.JwtResponse;
import ru.daniil.bulletinBoard.service.user.JwtService;
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
                      UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
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

                String jwtToken = jwtService.generateToken(user.get());
                return ResponseEntity.ok(new JwtResponse(jwtToken));
            }

            return ResponseEntity.badRequest().build();
        }
        catch (Exception e){
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Регистрация нового пользователя
     * @param registrationRequest заполненная форма регистрации
     * @param bindingResult результат валидации с фронта
     * @return путь для перенаправления
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationRequest registrationRequest,
                                          BindingResult bindingResult) {
        System.err.println("1");
        if (bindingResult.hasErrors()) {
            return ResponseEntity.internalServerError().build();
        }
        System.err.println("2");
        try {
            UserDetails user = userService.registerUser(registrationRequest);
            System.err.println("userService регистрации был вызван");
            String jwtToken = jwtService.generateToken(user);
            System.err.println("jwtService регистрации был вызван");
            return ResponseEntity.ok(new JwtResponse(jwtToken));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при создании");
        }
    }
}