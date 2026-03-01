package ru.daniil.bulletinBoard.service.user;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.daniil.bulletinBoard.entity.base.user.Role;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.enums.RoleName;
import ru.daniil.bulletinBoard.repository.user.RoleRepository;
import ru.daniil.bulletinBoard.repository.user.UserRepository;

import java.time.LocalDate;

@Transactional
@Service
public class UserAdminServiceImpl {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserAdminServiceImpl(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public User addRole(Long userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Роль " + roleName + " не найдена"));

        user.addRole(role);
        return userRepository.save(user);
    }

    public User removeRole(Long userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Роль " + roleName + " не найдена"));

        user.removeRole(role);
        return userRepository.save(user);
    }

    public User blockUser(Long userId, LocalDate blockedUntil) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setBlockedUntil(blockedUntil);
        return userRepository.save(user);
    }

    public User unblockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setBlockedUntil(null);
        return userRepository.save(user);
    }

    public User blockTrading(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setTradingBlocked(true);
        return userRepository.save(user);
    }

    public User unblockTrading(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setTradingBlocked(false);
        return userRepository.save(user);
    }
}
