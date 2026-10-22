package br.com.sistemabanco.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("dev")
public class SwaggerSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(regras -> regras
                        .anyRequest().permitAll());

        return http.build();
    }
}