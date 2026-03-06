package ru.daniil.bulletinBoard.config.PostConstructInit;

import jakarta.annotation.PostConstruct;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.daniil.bulletinBoard.entity.base.user.Role;
import ru.daniil.bulletinBoard.enums.RoleName;
import ru.daniil.bulletinBoard.repository.user.RoleRepository;

@Component
public class RolePostConstructInitializer {

    private final RoleRepository roleRepository;

    public RolePostConstructInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    @Order(1)
    public void initializeRoles() {
        createRoleIfNotExists(RoleName.USER, "Обычный пользователь системы");
        createRoleIfNotExists(RoleName.ADMIN, "Администратор системы");
    }

    private void createRoleIfNotExists(RoleName name, String description) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = new Role(name, description);
            roleRepository.save(role);
            System.out.println("Создана роль: " + name);
        }
    }
}