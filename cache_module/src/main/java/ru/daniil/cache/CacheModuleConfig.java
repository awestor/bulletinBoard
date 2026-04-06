package ru.daniil.cache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@EnableCaching
@ComponentScan(basePackages = "ru.daniil.cache")
@Order(2)
public class CacheModuleConfig {
}