package br.com.sistemabanco.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final ApiAuthenticationEntryPoint entryPoint;

    public SecurityConfig(ApiAuthenticationEntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filtrosDeSeguranca(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basico -> basico.authenticationEntryPoint(entryPoint))
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers(HttpMethod.POST, "/contas").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/style.css",
                                "/app.js",
                                "/css/**",
                                "/js/**",
                                "/assets/**"
                        ).permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/contas").hasRole("ADMIN")
                        .requestMatchers("/contas/admin/**").hasRole("ADMIN")
                        .requestMatchers("/contas/me/**", "/transferencias").hasRole("CLIENTE")
                        .anyRequest().denyAll());

        return http.build();
    }
}