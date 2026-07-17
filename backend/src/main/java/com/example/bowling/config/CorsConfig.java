package com.example.bowling.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:4200",  // Angular 16/18 versions
                    "http://localhost:4201"   // Angular 21 version
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
