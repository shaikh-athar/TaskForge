package com.taskforge.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

        private static final String AUTH_URL = "http://localhost:10091/realms/taskforge/protocol/openid-connect/auth";
        private static final String TOKEN_URL = "http://localhost:10091/realms/taskforge/protocol/openid-connect/token";

        @Bean
        public OpenAPI taskForgeOpenAPI() {
                return new OpenAPI()
                                .info(new Info().title("TaskForge API")
                                                .description("TaskForge Project Management API")
                                                .version("v0.0.1")
                                                .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                                .addSecurityItem(new SecurityRequirement().addList("Keycloak"))
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                                .components(new Components()
                                                .addSecuritySchemes("Keycloak",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.OAUTH2)
                                                                                .description("OAuth2 Flow with Keycloak")
                                                                                .flows(new OAuthFlows()
                                                                                                .authorizationCode(
                                                                                                                new OAuthFlow()
                                                                                                                                .authorizationUrl(
                                                                                                                                                AUTH_URL)
                                                                                                                                .tokenUrl(TOKEN_URL)
                                                                                                                                .scopes(new Scopes()
                                                                                                                                                .addString("openid",
                                                                                                                                                                "openid")
                                                                                                                                                .addString("profile",
                                                                                                                                                                "profile")
                                                                                                                                                .addString("email",
                                                                                                                                                                "email")))))
                                                .addSecuritySchemes("bearerAuth",
                                                                new SecurityScheme()
                                                                                .name("bearerAuth")
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")));
        }
}
