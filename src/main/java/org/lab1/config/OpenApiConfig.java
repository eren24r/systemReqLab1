package org.lab1.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI uniCoFoundOpenApi() {
        return new OpenAPI().info(new Info()
                .title("UniCoFound API")
                .version("1.0.0")
                .description("Platform for student startups: projects, co-founder search, team formation and investment offers")
                .contact(new Contact().name("UniCoFound team")));
    }
}
