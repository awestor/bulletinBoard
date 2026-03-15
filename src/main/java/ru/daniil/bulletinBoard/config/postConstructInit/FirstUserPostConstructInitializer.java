package ru.daniil.bulletinBoard.config.postConstructInit;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.enums.RoleName;
import ru.daniil.bulletinBoard.repository.user.RoleRepository;
import ru.daniil.bulletinBoard.repository.user.UserRepository;

@Component
public class FirstUserPostConstructInitializer implements ApplicationRunner {
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

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //initializeFirstUser();
    }
}
