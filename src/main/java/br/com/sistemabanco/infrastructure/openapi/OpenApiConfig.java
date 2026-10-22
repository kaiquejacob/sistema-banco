package br.com.sistemabanco.infrastructure.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI sistemaBancoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Banco API")
                        .description("API REST para contas bancárias, depósitos, saques, transferências e operações administrativas.")
                        .version("1.0.0")
                        .contact(new Contact().name("Kaíque Jacob")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BASIC_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")));
    }
}