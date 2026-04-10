package ru.daniil.user.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "ru.daniil.user")
@EnableJpaRepositories(basePackages = "ru.daniil.user.repository")
@EntityScan(basePackages = "ru.daniil.core.entity")
@Order(4)
public class UserModuleConfig {
    // Этот класс инициализирует компоненты user_module
}