package com.example.vibe1.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Spring Boot's RestClient.Builder auto-configuration isn't active in this
 * app (confirmed: injecting it directly throws NoSuchBeanDefinitionException),
 * so any module using RestClient needs this bean explicitly.
 */
@Configuration
class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
