package ru.daniil.discount.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "ru.daniil.discount")
@EnableJpaRepositories(basePackages = "ru.daniil.discount.repository")
@EntityScan(basePackages = "ru.daniil.core.entity")
@Order(4)
public class DiscountModuleConfig {
    // Этот класс инициализирует компоненты discount_module
}