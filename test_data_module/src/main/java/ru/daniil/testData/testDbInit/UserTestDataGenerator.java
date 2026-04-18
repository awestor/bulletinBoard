package ru.daniil.testData.testDbInit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.enums.AuthProvider;
import ru.daniil.core.request.auth.RegistrationRequest;
import ru.daniil.user.service.user.UserService;
import ru.daniil.user.service.user.UserTestDataService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserTestDataGenerator {

    private static final Logger infoLogger = LoggerFactory.getLogger("INFO-LOGGER");

    private static final String TEST_USER_PREFIX = "generated-test-";

    //Нужен для того чтобы всегда можно было проверить внутрянку продуктов у продавцов
    private static final String FIRST_SELLER_LOGIN = "generated-test-first-seller";
    private static final String FIRST_SELLER_EMAIL = "first_seller@test.com";
    private static final String FIRST_SELLER_PASSWORD = "TestPassword123!";

    private final UserService userService;
    private final UserTestDataService userTestDataService;

    public UserTestDataGenerator(UserService userService, UserTestDataService userTestDataService) {
        this.userService = userService;
        this.userTestDataService = userTestDataService;
    }

    /**
     * Генерирует тестовых пользователей-продавцов
     * @param count количество пользователей для генерации (включая первого фиксированного)
     * @return список username созданных пользователей
     */
    @Transactional
    public List<String> generateTestSellers(int count) {
        List<String> users = new ArrayList<>();

        if (count <= 0) {
            infoLogger.warn("Количество пользователей для генерации должно быть больше 0");
            return users;
        }

        infoLogger.info("Начинаем генерацию {} тестовых продавцов...", count);

        try {
            if (!userService.existsByLogin(FIRST_SELLER_LOGIN)) {
                RegistrationRequest firstRequest = RegistrationRequest.builder()
                        .login(FIRST_SELLER_LOGIN)
                        .email(FIRST_SELLER_EMAIL)
                        .password(FIRST_SELLER_PASSWORD)
                        .authProvider(AuthProvider.LOCAL)
                        .build();

                var firstUser = userService.registerUser(firstRequest);
                users.add(firstUser.getUsername());
                infoLogger.info("Создан первый продавец: {}", FIRST_SELLER_LOGIN);
            }
            count--;
        } catch (Exception e) {
            infoLogger.error("Ошибка создания первого продавца {}: {}", FIRST_SELLER_LOGIN, e.getMessage());
        }

        for (int i = 0; i < count; i++) {
            try {
                String login = generateUniqueTestLogin(i);
                String email = generateEmail(login);
                String password = "TestPassword123!";

                System.err.println(email);

                RegistrationRequest request = new RegistrationRequest(
                        login, email, password
                );
                request.setAuthProvider(AuthProvider.LOCAL);

                var userDetails = userService.registerUser(request);
                users.add(userDetails.getUsername());
                infoLogger.debug("Создан тестовый продавец: {}", login);

            } catch (Exception e) {
                infoLogger.error("Ошибка создания пользователя: {}", e.getMessage());
            }
        }

        infoLogger.info("Создано {} продавцов (включая основного)", users.size());
        return users;
    }

    /**
     * Генерирует уникальный логин для тестового пользователя.
     * Использует префикс + UUID для гарантии уникальности
     * @return сгенерированное имя
     */
    private String generateUniqueTestLogin(int index) {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return TEST_USER_PREFIX + uniqueId + "-" + index;
    }

    /**
     * Генерирует email для тестового пользователя
     * @return сгенерированная почта
     */
    private String generateEmail(String login) {
        return login + "@generated.com";
    }

    /**
     * Удаляет всех тестовых пользователей через массовый запрос
     * @return количество удалённых пользователей
     */
    @Transactional
    public int deleteGeneratedTestUsers() {
        long countBefore = userTestDataService.countByLoginStartingWith(TEST_USER_PREFIX);

        if (countBefore == 0) {
            infoLogger.info("Нет тестовых пользователей для удаления (префикс: {})", TEST_USER_PREFIX);
            return 0;
        }

        int deletedCount = userTestDataService.deleteAllByLoginStartingWith(TEST_USER_PREFIX);

        infoLogger.info("Удалено {} тестовых пользователей", deletedCount);
        return deletedCount;
    }

    /**
     * Возвращает логин первого продавца
     * @return имя первого сгенерированного пользователя
     */
    public String getFirstSellerLogin() {
        return FIRST_SELLER_LOGIN;
    }

    /**
     * Возвращает тестовый префикс
     * @return префикс тестового пользователя
     */
    public String getTestUserPrefix() {
        return TEST_USER_PREFIX;
    }

    /**
     * Возвращает количество тестовых пользователей в БД
     * @return число сгенерированных пользователей, что ещё остались в БД
     */
    public Long getTestUsersCount() {
        return userTestDataService.countByLoginStartingWith(TEST_USER_PREFIX);
    }

    /**
     * Возвращает список имён находящихся в БД тестовых продавцов
     * @return список имён тестовых продавцов
     */
    public List<String> getGeneratedTestUsersList() {
        return userTestDataService.findAllByLoginStartingWith(TEST_USER_PREFIX)
                .stream()
                .map(User::getLogin)
                .collect(Collectors.toList());
    }
}