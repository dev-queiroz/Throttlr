package com.example.ratelimiter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI throttlrOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Throttlr API")
                        .version("v1")
                        .description("Distributed Rate Limiting and Quota System with hierarchical policies."));
    }
}
