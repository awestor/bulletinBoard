package ru.daniil.testData.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.daniil.testData.testDbInit.UserTestDataGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test/users")
@Tag(name = "Управление тестовыми пользователями",
        description = "Endpoints для генерации и управления тестовыми пользователями-продавцами")
public class UserTestDataController {

    private static final Logger infoLogger = LoggerFactory.getLogger("INFO-LOGGER");
    private final UserTestDataGenerator userTestDataGenerator;

    public UserTestDataController(UserTestDataGenerator userTestDataGenerator) {
        this.userTestDataGenerator = userTestDataGenerator;
    }

    @PostMapping("/generate")
    @Operation(
            summary = "Генерация тестовых продавцов",
            description = """
                    Создаёт тестовых пользователей с ролью продавца для тестирования функционала продуктов.
                    
                    **Структура генерируемых пользователей:**
                    - **Первый пользователь** всегда создаётся с фиксированным логином `generated-test-first-seller`
                    - **Остальные пользователи** генерируются с префиксом `generated-test-{UUID}-{index}`
                    
                    **Параметры генерации:**
                    - Пароль для всех пользователей: `TestPassword123!`
                    - Email генерируется на основе логина
                    
                    **Примечание:** При повторном вызове фиксированный пользователь не дублируется,
                    а остальные создаются только если общее количество меньше запрошенного.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Количество продавцов для генерации",
                    content = @Content(mediaType = "application/json")
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователи успешно созданы",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректное количество пользователей (должно быть > 0)",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера при создании пользователей",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> generateTestSellers(
            @Parameter(description = "Количество продавцов для генерации", required = true)
            @RequestParam(defaultValue = "5") int count) {

        Map<String, Object> response = new HashMap<>();

        if (count <= 0) {
            response.put("error", "Количество пользователей должно быть больше 0");
            response.put("status", "BAD_REQUEST");
            return ResponseEntity.badRequest().body(response);
        }

        infoLogger.info("Запрос на генерацию {} тестовых продавцов", count);

        try {
            List<String> createdUsers = userTestDataGenerator.generateTestSellers(count);

            response.put("message", "Тестовые продавцы успешно созданы");
            response.put("totalGenerated", createdUsers.size());
            response.put("requestedCount", count);
            response.put("createdUsers", createdUsers);
            response.put("mainSellerLogin", userTestDataGenerator.getFirstSellerLogin());
            response.put("totalTestUsersInDb", userTestDataGenerator.getTestUsersCount());

            infoLogger.info("Создано {} продавцов из {} запрошенных", createdUsers.size(), count);

            return ResponseEntity.ok(response);
        } catch (Exception e){
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @DeleteMapping("/clear")
    @Operation(
            summary = "Очистка тестовых пользователей",
            description = """
                    Удаляет всех тестовых пользователей, созданных генератором.
                    
                    **Что удаляется:**
                    - Все пользователи с префиксом `generated-test-*` и первый тестовый пользователь
                    
                    **Что НЕ удаляется:**
                    - Пользователи без префикса `generated-test-`
                    - Пользователи из keycloak
                    
                    **Предупреждение:** Перед удалением пользователей рекомендуется сначала удалить их продукты,
                    чтобы избежать нарушений внешних ключей.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Тестовые пользователи успешно удалены",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Ошибка при удалении пользователей",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> clearTestUsers() {

        Map<String, Object> response = new HashMap<>();

        infoLogger.info("Запрос на удаление тестовых пользователей");

        long usersBefore = userTestDataGenerator.getTestUsersCount();

        int deletedCount = userTestDataGenerator.deleteGeneratedTestUsers();

        response.put("message", "Тестовые пользователи удалены");
        response.put("usersDeleted", deletedCount);
        response.put("usersBefore", usersBefore);
        response.put("remainingTestUsers", userTestDataGenerator.getTestUsersCount());

        infoLogger.info("Удалено {} тестовых пользователей", deletedCount);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Получение статистики тестовых пользователей",
            description = """
                    Возвращает детальную статистику по тестовым пользователям:
                    - Общее количество тестовых пользователей в БД
                    - Логин основного продавца
                    - Список всех созданных тестовых логинов
                    - Префикс для идентификации тестовых пользователей
                    
                    Используется для проверки готовности данных к API тестам и мониторинга.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Статистика успешно получена",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> getTestUsersStats() {
        Map<String, Object> stats = new HashMap<>();

        long testUsersCount = userTestDataGenerator.getTestUsersCount();

        stats.put("totalTestUsers", testUsersCount);
        stats.put("testUserPrefix", userTestDataGenerator.getTestUserPrefix());
        stats.put("mainSellerLogin", userTestDataGenerator.getFirstSellerLogin());

        if (testUsersCount > 0) {
            stats.put("message", "Тестовые пользователи готовы к использованию");
            stats.put("hasTestData", true);
        } else {
            stats.put("message", "Тестовые пользователи не созданы. Выполните POST /api/test/users/generate");
            stats.put("hasTestData", false);
        }

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/list")
    @Operation(
            summary = "Получение списка тестовых пользователей",
            description = "Возвращает список логинов всех тестовых пользователей, созданных в системе"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список успешно получен",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> listTestUsers() {
        Map<String, Object> response = new HashMap<>();

        List<String> testUsers = userTestDataGenerator.getGeneratedTestUsersList();

        response.put("testUsers", testUsers);
        response.put("total", testUsers.size());
        response.put("firstSellerLogin", userTestDataGenerator.getFirstSellerLogin());

        return ResponseEntity.ok(response);
    }
}