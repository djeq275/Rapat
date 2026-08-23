package com.example.vibe1.calendar;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

/**
 * Wiring for the persistent, encrypted "google-calendar" authorized client:
 * - {@link OAuth2AuthorizedClientRepository} bean routes that one registration to
 *   the encrypted DB store (see {@link RegistrationRoutingAuthorizedClientRepository})
 *   while everything else (google-login) keeps Spring's default session storage.
 * - {@link OAuth2AuthorizedClientManager} bean lets background code (the calendar
 *   sync listener/retry, outside any HTTP request) fetch/refresh a token by
 *   principal name alone.
 */
@Configuration
class CalendarClientConfig {

    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository(GoogleAuthorizedClientService googleAuthorizedClientService) {
        return new RegistrationRoutingAuthorizedClientRepository(googleAuthorizedClientService);
    }

    @Bean
    OAuth2AuthorizedClientManager backgroundAuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            GoogleAuthorizedClientService googleAuthorizedClientService) {
        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .refreshToken()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, googleAuthorizedClientService);
        manager.setAuthorizedClientProvider(authorizedClientProvider);
        return manager;
    }
}
