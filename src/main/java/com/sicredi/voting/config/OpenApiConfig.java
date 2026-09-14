package com.sicredi.voting.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI votingApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Voting API")
                        .description("REST API for cooperative assembly topics, voting sessions and votes")
                        .version("v1"));
    }
}
