package com.example.bowling.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Bowling Game API
 *
 * This configuration automatically generates interactive API documentation
 * accessible at /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bowlingGameOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bowling Game API - Spring Boot Backend")
                        .description("""
                                Spring Boot backend for the Bowling Game Kata.

                                **Features:**
                                - RESTful API design
                                - Input validation
                                - Comprehensive error handling
                                - Game state management
                                - Full test coverage (48 tests)

                                **Bowling Rules:**
                                - 10 frames per game
                                - Strikes: 10 pins in first roll of frame
                                - Spares: 10 pins using both rolls of frame
                                - 10th frame bonus rolls for strikes/spares
                                - Maximum score: 300 (perfect game)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Bowling Game API")
                                .email("support@example.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server"),
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("API base path")
                ));
    }
}
