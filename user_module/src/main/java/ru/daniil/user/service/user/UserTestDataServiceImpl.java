package ru.daniil.user.service.user;

import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.user.repository.UserRepository;

import java.util.List;

@Service
public class UserTestDataServiceImpl implements UserTestDataService {
    private final UserRepository userRepository;

    public UserTestDataServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> findAllByLoginStartingWith(String prefix) {
        return userRepository.findAllByLoginStartingWith(prefix);
    }

    @Override
    public Long countByLoginStartingWith(String prefix) {
        return userRepository.countByLoginStartingWith(prefix);
    }

    @Override
    public Integer deleteAllByLoginStartingWith(String prefix) {
        return userRepository.deleteAllByLoginStartingWith(prefix);
    }
}
