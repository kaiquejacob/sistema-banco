package br.com.sistemabanco.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "viacep")
public record ViaCepProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}