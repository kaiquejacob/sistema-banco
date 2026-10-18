package br.com.sistemabanco.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(ViaCepProperties.class)
public class CepClientConfig {

    @Bean
    public RestClient viaCepRestClient(ViaCepProperties propriedades) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(propriedades.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(propriedades.readTimeout());

        return RestClient.builder()
                .baseUrl(propriedades.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}