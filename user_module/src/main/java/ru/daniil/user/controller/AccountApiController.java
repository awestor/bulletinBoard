package ru.daniil.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.daniil.user.service.user.UserService;

import java.util.Map;

@Controller
public class AccountApiController {
    private final UserService userService;

    public AccountApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/users/{username}/avatar")
    public ResponseEntity<?> getUserAvatar(@PathVariable String username) {
        try {
            String avatarUrl = userService.getUserAvatar(username);
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("avatarUrl", null));
        }
    }
}
