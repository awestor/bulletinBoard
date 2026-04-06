package ru.daniil.order.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "ru.daniil.order")
@EnableJpaRepositories(basePackages = "ru.daniil.order.repository")
@EntityScan(basePackages = "ru.daniil.core.entity")
@Order(5)
public class OrderModuleConfig {
    // Этот класс инициализирует компоненты user_module
}