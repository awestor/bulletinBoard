package ru.daniil.comment.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@Configuration
@ComponentScan(basePackages = "ru.daniil.comment")
@EnableJpaRepositories(basePackages = "ru.daniil.comment.repository")
@EntityScan(basePackages = "ru.daniil.core.entity")
@Order(5)
public class CommentModuleConfig {
}
