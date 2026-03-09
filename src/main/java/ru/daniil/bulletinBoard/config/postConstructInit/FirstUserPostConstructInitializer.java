package ru.daniil.bulletinBoard.config.postConstructInit;

import jakarta.annotation.PostConstruct;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.enums.RoleName;
import ru.daniil.bulletinBoard.repository.user.RoleRepository;
import ru.daniil.bulletinBoard.repository.user.UserRepository;

@Component
public class FirstUserPostConstructInitializer {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public FirstUserPostConstructInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @PostConstruct
    @Order(2)
    public void initializeFirstUser() {
        createMealTypeIfFirstUser();
    }

    private void createMealTypeIfFirstUser() {
        if (userRepository.count() == 0) {
            User user = new User();
            user.setEmail("firstUser@example.com");
            user.setLogin("firstUser");
            user.setPassword(passwordEncoder.encode("Password123!"));

            roleRepository.findByName(RoleName.USER).ifPresent(user::addRole);
            roleRepository.findByName(RoleName.ADMIN).ifPresent(user::addRole);

            userRepository.save(user);
            System.out.println("Создан первый пользователь с ролями USER и ADMIN");
        }
    }
}
