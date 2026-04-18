package ru.daniil.user.service.user;

import ru.daniil.core.entity.base.user.User;

import java.util.List;

public interface UserTestDataService {

    List<User> findAllByLoginStartingWith(String prefix);

    Long countByLoginStartingWith(String prefix);

    Integer deleteAllByLoginStartingWith(String prefix);
}
