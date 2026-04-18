package ru.daniil.core.sharedInterfaces;

import ru.daniil.core.entity.base.user.User;

import java.util.Optional;

public interface UserProvider {
    /**
     * Возвращает пользователя по его логину
     * @param login логин пользователя
     * @return Пользователь или null
     */
    Optional<User> getByLogin(String login);

    /**
     * Возвращает пользователя по его почте
     * @param email почта пользователя
     * @return Пользователь или null
     */
    Optional<User> getByEmail(String email);


    /**
     * Проверяет существование пользователя по email
     * @param email электронная почта пользователя
     * @return true - пользователя найден, иначе false
     */
    boolean existsByEmail(String email);

    /**
     * Проверяет существование пользователя по login
     * @param login - имя пользователя
     * @return true - пользователя найден, иначе false
     */
    boolean existsByLogin(String login);
}
