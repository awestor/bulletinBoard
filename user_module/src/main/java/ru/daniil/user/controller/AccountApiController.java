package ru.daniil.user.controller;

import jakarta.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.daniil.user.service.user.UserService;

import java.util.Map;

@Controller
public class AccountApiController {
    private final UserService userService;
    private static final Logger infoLogger = LoggerFactory.getLogger("INFO-LOGGER");

    public AccountApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/users/avatar")
    public ResponseEntity<?> getUserAvatar() {
        try {
            JwtAuthenticationToken authentication = (JwtAuthenticationToken)
                    SecurityContextHolder.getContext().getAuthentication();

            assert authentication != null;
            Jwt jwt = authentication.getToken();
            String email = jwt.getClaimAsString("email");
            infoLogger.info("Username = {}", email);

            String avatarUrl = userService.getUserAvatar(email);
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            return ResponseEntity.ok(Map.of("avatarUrl", "none"));
        }
    }
}
