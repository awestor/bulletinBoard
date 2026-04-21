package ru.daniil.user.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.image.service.user.UserImageService;
import ru.daniil.user.service.user.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/user-images")
@Tag(name = "Api для работы с аватаркой пользователя", description = "API для работы с аватаркой пользователя")
public class UserImageApiController {
    private final UserService userService;
    private final UserImageService userImageService;

    public UserImageApiController(UserService userService, UserImageService userImageService) {
        this.userService = userService;
        this.userImageService = userImageService;
    }

    @GetMapping("/avatar")
    @Operation(
            summary = "Получение аватара пользователя",
            description = "Возвращает URL аватара текущего авторизованного пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "URL аватара успешно получен",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Пользователь не найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getUserAvatar() {
        try {
            JwtAuthenticationToken authentication = (JwtAuthenticationToken)
                    SecurityContextHolder.getContext().getAuthentication();

            assert authentication != null;
            Jwt jwt = authentication.getToken();
            String email = jwt.getClaimAsString("email");

            String avatarUrl = userService.getUserAvatar(email);
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            return ResponseEntity.ok(Map.of("avatarUrl", "none"));
        }
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Загрузка аватара пользователя",
            description = "Загружает новый аватар для текущего авторизованного пользователя. " +
                    "Старый аватар автоматически удаляется"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Добавление произведено успешно",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Файл не предоставлен или имеет неверный формат",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "Файл слишком большого размера",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера при загрузке файла",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @Parameter(
                    description = "Файл изображения для аватара (JPEG, PNG, GIF, WEBP)",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart("avatar")
            @ArraySchema(schema = @Schema(type = "string", format = "binary"))
            MultipartFile avatar) {

        try {
            User user = userService.getAuthUser();
            String avatarUrl = userImageService.saveImage(avatar, user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "avatarUrl", avatarUrl,
                    "message", "Аватар успешно обновлен"
            ));
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Пользователь не найден"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/avatar")
    @Operation(
            summary = "Удаление аватара пользователя",
            description = "Удаляет текущий аватар пользователя. После удаления будет использоваться стандартный аватар"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Удаление прошло успешно",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Пользователь не найден или аватар отсутствует",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера при удалении файла",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, String>> deleteAvatar() {
        try {
            User user = userService.getAuthUser();
            userImageService.deleteUserAvatar(user.getEmail(), user.getImageName());

            return ResponseEntity.ok(Map.of(
                    "message", "Аватар успешно удален",
                    "avatarUrl", "none"
            ));
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Пользователь не найден или аватар отсутствует"
            ));
        }
    }

}