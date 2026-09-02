package com.example.vibe1.security;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicClientRegistrationRepositoryTest {

    @Mock
    KeycloakConfigService keycloakConfigService;

    DynamicClientRegistrationRepository repository;

    @Test
    void staticGoogleRegistrationsAreServedFromPropertiesWithoutTouchingKeycloakConfig() {
        repository = new DynamicClientRegistrationRepository(googlePropertiesFixture(), keycloakConfigService);

        ClientRegistration registration = repository.findByRegistrationId("google-login");

        assertThat(registration).isNotNull();
        assertThat(registration.getClientId()).isEqualTo("google-client-id");
        verifyNoInteractions(keycloakConfigService);
    }

    @Test
    void unknownRegistrationIdReturnsNull() {
        repository = new DynamicClientRegistrationRepository(new OAuth2ClientProperties(), keycloakConfigService);

        assertThat(repository.findByRegistrationId("does-not-exist")).isNull();
    }

    @Test
    void keycloakLookupReturnsNullWhenNotConfigured() {
        repository = new DynamicClientRegistrationRepository(new OAuth2ClientProperties(), keycloakConfigService);
        when(keycloakConfigService.currentConfig()).thenReturn(Optional.empty());

        assertThat(repository.findByRegistrationId("keycloak")).isNull();
    }

    @Test
    void cachesSuccessfulKeycloakRegistrationUntilConfigSavedEventInvalidatesIt() {
        KeycloakConfig config = new KeycloakConfig();
        config.setServerUrl("https://keycloak.local");
        config.setRealm("company");
        config.setClientId("rapat-app");
        config.setClientSecretEnc("secret");
        when(keycloakConfigService.currentConfig()).thenReturn(Optional.of(config));

        ClientRegistration stub = stubClientRegistration();
        AtomicInteger buildCount = new AtomicInteger();
        repository = new DynamicClientRegistrationRepository(new OAuth2ClientProperties(), keycloakConfigService) {
            @Override
            ClientRegistration toClientRegistration(KeycloakConfig cfg) {
                buildCount.incrementAndGet();
                return stub;
            }
        };

        assertThat(repository.findByRegistrationId("keycloak")).isSameAs(stub);
        assertThat(repository.findByRegistrationId("keycloak")).isSameAs(stub);
        assertThat(buildCount.get()).isEqualTo(1);

        repository.onKeycloakConfigSaved(new KeycloakConfigSavedEvent());
        assertThat(repository.findByRegistrationId("keycloak")).isSameAs(stub);
        assertThat(buildCount.get()).isEqualTo(2);
    }

    private static OAuth2ClientProperties googlePropertiesFixture() {
        OAuth2ClientProperties properties = new OAuth2ClientProperties();
        OAuth2ClientProperties.Registration googleLogin = new OAuth2ClientProperties.Registration();
        googleLogin.setProvider("google");
        googleLogin.setClientId("google-client-id");
        googleLogin.setClientSecret("google-client-secret");
        googleLogin.setScope(Set.of("openid", "email", "profile"));
        properties.getRegistration().put("google-login", googleLogin);
        return properties;
    }

    private static ClientRegistration stubClientRegistration() {
        return ClientRegistration.withRegistrationId("keycloak")
                .clientId("rapat-app")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://keycloak.local/authorize")
                .tokenUri("https://keycloak.local/token")
                .build();
    }
}
