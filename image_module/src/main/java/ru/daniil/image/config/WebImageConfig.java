package ru.daniil.image.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebImageConfig implements WebMvcConfigurer {

    @Value("${file.user.upload-dir}")
    private String userUploadDir;

    @Value("${file.product.upload-dir}")
    private String productUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/users/**")
                .addResourceLocations("file:" + userUploadDir)
                .setCachePeriod(3600);

        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations("file:" + productUploadDir)
                .setCachePeriod(3600);
    }
}