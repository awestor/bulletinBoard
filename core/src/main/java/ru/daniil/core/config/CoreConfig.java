package ru.daniil.core.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@ComponentScan(basePackages = "ru.daniil.core")
@Order(1)
public class CoreConfig {
}