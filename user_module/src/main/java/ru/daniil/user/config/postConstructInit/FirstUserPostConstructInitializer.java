package ru.daniil.user.config.postConstructInit;

import jakarta.annotation.PostConstruct;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.user.repository.UserRepository;


@Component
public class FirstUserPostConstructInitializer {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public FirstUserPostConstructInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Order(2)
    public void initializeFirstUser() {
        //createMealTypeIfFirstUser();
    }

    private void createMealTypeIfFirstUser() {
        if (userRepository.count() == 0) {
            User user = new User();
            user.setEmail("firstUser@example.com");
            user.setLogin("firstUser");
            user.setPassword(passwordEncoder.encode("Password123!"));

            userRepository.save(user);
            System.out.println("Создан первый пользователь");
        }
    }

}
