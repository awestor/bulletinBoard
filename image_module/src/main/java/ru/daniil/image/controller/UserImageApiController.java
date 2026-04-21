package ru.daniil.image.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-images")
@Tag(name = "Api для работы с аватаркой пользователя", description = "API для работы с аватаркой пользователя")
public class UserImageApiController {
}
