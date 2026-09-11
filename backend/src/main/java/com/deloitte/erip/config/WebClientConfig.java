package com.deloitte.erip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${erip.ml-service.base-url}")
    private String mlServiceBaseUrl;

    @Bean
    public WebClient mlServiceWebClient() {
        return WebClient.builder()
                .baseUrl(mlServiceBaseUrl)
                .build();
    }
}
