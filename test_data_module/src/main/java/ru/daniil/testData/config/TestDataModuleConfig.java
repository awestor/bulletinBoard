package ru.daniil.testData.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@ComponentScan(basePackages = "ru.daniil.testData")
@EntityScan(basePackages = "ru.daniil.core.entity")
@Order(6)
public class TestDataModuleConfig {
    // Этот класс инициализирует компоненты test_data_module
}
