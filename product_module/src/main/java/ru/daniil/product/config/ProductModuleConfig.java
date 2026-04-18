package ru.daniil.product.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "ru.daniil.product")
@EnableJpaRepositories(basePackages = "ru.daniil.product.repository")
@EntityScan(basePackages = "ru.daniil.core.entity")
@Order(4)
public class ProductModuleConfig {
    // Этот класс инициализирует компоненты product_module
}