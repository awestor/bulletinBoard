package ru.daniil.user.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.user.User;

import java.util.List;
import java.util.Optional;

@Repository
@RepositoryRestResource(path = "users")
public interface UserRepository extends CrudRepository<User, Long> {
    /**
     * Находит пользователя по его почте
     * @param email почта пользователя
     * @return найденный или нет пользователь
     */
    Optional<User> findByEmail(String email);

    /**
     * Находит пользователя по его логину
     * @param login логин пользователя
     * @return найденный или нет пользователь
     */
    Optional<User> findByLogin(String login);

    /**
     * Удаляет всех пользователей, чей логин начинается с указанного префикса
     * @param prefix префикс для поиска (например "generated-test-")
     * @return количество удалённых пользователей
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.login LIKE CONCAT(:prefix, '%')")
    int deleteAllByLoginStartingWith(@Param("prefix") String prefix);

    /**
     * Получает количество пользователей с указанным префиксом
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.login LIKE CONCAT(:prefix, '%')")
    long countByLoginStartingWith(@Param("prefix") String prefix);

    /**
     * Находит всех пользователей с логином, начинающимся с указанного префикса
     * @return список всех пользователей, что начинаются с данного префикса
     */
    List<User> findAllByLoginStartingWith(String prefix);

    /**
     * Проверяет существование пользователя по его почте
     * @param email логин пользователя
     * @return true ли false
     */
    boolean existsByEmail(String email);

    /**
     * Проверяет существование пользователя по его логину
     * @param login логин пользователя
     * @return true ли false
     */
    boolean existsByLogin(String login);
}
