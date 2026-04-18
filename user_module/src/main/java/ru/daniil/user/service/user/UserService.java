package ru.daniil.user.service.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.auth.RegistrationRequest;
import ru.daniil.core.sharedInterfaces.UserProvider;

@Service
public interface UserService extends UserProvider {

    /**
     * Регистрирует нового пользователя в системе и назначает ему права роли "USER"
     *
     * @param request RegistrationRequest что содержит регистрационные данные
     */
    UserDetails registerUser(RegistrationRequest request);

    /**
     * Регистрирует нового пользователя в системе и назначает ему права роли "USER"
     * Но не валидирует данные реквеста
     * Нужен для сохранения новых пользователей, что зарегистрировались через внешнего провайдера
     *
     * @param request RegistrationRequest что содержит регистрационные данные
     */
    UserDetails registerUserWithoutValidation(RegistrationRequest request);

    /**
     * Получает пользователя из если он авторизован
     * @return объект сущности пользователя или ошибка
     */
    User getAuthUser();

    String getUserAvatar(String username);
}
