package ru.daniil.user.service.user;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.user.repository.UserRepository;

import java.time.LocalDate;

@Transactional
@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;

    public UserAdminServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User blockUser(Long userId, LocalDate blockedUntil) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setBlockedUntil(blockedUntil);
        return userRepository.save(user);
    }

    @Override
    public User unblockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setBlockedUntil(null);
        return userRepository.save(user);
    }

    @Override
    public User blockTrading(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setTradingBlocked(true);
        return userRepository.save(user);
    }

    @Override
    public User unblockTrading(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setTradingBlocked(false);
        return userRepository.save(user);
    }
}
