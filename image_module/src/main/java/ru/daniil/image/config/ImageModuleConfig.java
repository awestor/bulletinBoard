package ru.daniil.image.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "ru.daniil.image")
@EnableJpaRepositories(basePackages = "ru.daniil.image.repository")
@EntityScan(basePackages = "ru.daniil.core.entity")
@Order(3)
public class ImageModuleConfig {
    // Этот класс инициализирует компоненты user_module
}