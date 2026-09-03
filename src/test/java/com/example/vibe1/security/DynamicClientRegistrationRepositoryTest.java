package com.example.vibe1.security;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicClientRegistrationRepositoryTest {

    @Mock
    KeycloakConfigService keycloakConfigService;
    @Mock
    GoogleOAuthConfigService googleOAuthConfigService;

    DynamicClientRegistrationRepository repository;

    @Test
    void unknownRegistrationIdReturnsNull() {
        repository = new DynamicClientRegistrationRepository(keycloakConfigService, googleOAuthConfigService);

        assertThat(repository.findByRegistrationId("does-not-exist")).isNull();
    }

    @Test
    void keycloakLookupReturnsNullWhenNotConfigured() {
        repository = new DynamicClientRegistrationRepository(keycloakConfigService, googleOAuthConfigService);
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

        ClientRegistration stub = stubKeycloakClientRegistration();
        AtomicInteger buildCount = new AtomicInteger();
        repository = new DynamicClientRegistrationRepository(keycloakConfigService, googleOAuthConfigService) {
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

    @Test
    void googleLoginLookupReturnsNullWhenNotConfigured() {
        repository = new DynamicClientRegistrationRepository(keycloakConfigService, googleOAuthConfigService);
        when(googleOAuthConfigService.currentConfig()).thenReturn(Optional.empty());

        assertThat(repository.findByRegistrationId("google-login")).isNull();
    }

    @Test
    void googleCalendarLookupReturnsNullWhenNotConfigured() {
        repository = new DynamicClientRegistrationRepository(keycloakConfigService, googleOAuthConfigService);
        when(googleOAuthConfigService.currentConfig()).thenReturn(Optional.empty());

        assertThat(repository.findByRegistrationId("google-calendar")).isNull();
    }

    @Test
    void buildsGoogleLoginRegistrationWithIdentityScopeAndSharedCredentials() {
        repository = new DynamicClientRegistrationRepository(keycloakConfigService, googleOAuthConfigService);
        when(googleOAuthConfigService.currentConfig()).thenReturn(Optional.of(googleConfigFixture()));

        ClientRegistration registration = repository.findByRegistrationId("google-login");

        assertThat(registration).isNotNull();
        assertThat(registration.getClientId()).isEqualTo("google-client-id");
        assertThat(registration.getClientSecret()).isEqualTo("google-client-secret");
        assertThat(registration.getScopes()).containsExactlyInAnyOrder("openid", "email", "profile");
    }

    @Test
    void buildsGoogleCalendarRegistrationWithNarrowerScopeAndRedirectUriOverride() {
        repository = new DynamicClientRegistrationRepository(keycloakConfigService, googleOAuthConfigService);
        when(googleOAuthConfigService.currentConfig()).thenReturn(Optional.of(googleConfigFixture()));

        ClientRegistration registration = repository.findByRegistrationId("google-calendar");

        assertThat(registration).isNotNull();
        assertThat(registration.getClientId()).isEqualTo("google-client-id");
        assertThat(registration.getClientSecret()).isEqualTo("google-client-secret");
        assertThat(registration.getScopes()).containsExactly("https://www.googleapis.com/auth/calendar.events");
        assertThat(registration.getRedirectUri()).isEqualTo("{baseUrl}/connect/google-calendar");
        assertThat(registration.getAuthorizationGrantType()).isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
    }

    @Test
    void cachesGoogleRegistrationsUntilConfigSavedEventInvalidatesBoth() {
        repository = new DynamicClientRegistrationRepository(keycloakConfigService, googleOAuthConfigService);
        when(googleOAuthConfigService.currentConfig()).thenReturn(Optional.of(googleConfigFixture()));

        repository.findByRegistrationId("google-login");
        repository.findByRegistrationId("google-login");
        repository.findByRegistrationId("google-calendar");
        repository.findByRegistrationId("google-calendar");
        verify(googleOAuthConfigService, times(2)).currentConfig();

        repository.onGoogleOAuthConfigSaved(new GoogleOAuthConfigSavedEvent());
        repository.findByRegistrationId("google-login");
        repository.findByRegistrationId("google-calendar");
        verify(googleOAuthConfigService, times(4)).currentConfig();
    }

    private static GoogleOAuthConfig googleConfigFixture() {
        GoogleOAuthConfig config = new GoogleOAuthConfig();
        config.setClientId("google-client-id");
        config.setClientSecretEnc("google-client-secret");
        return config;
    }

    private static ClientRegistration stubKeycloakClientRegistration() {
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
