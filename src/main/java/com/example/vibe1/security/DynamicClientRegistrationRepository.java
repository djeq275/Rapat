package com.example.vibe1.security;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper;
import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Replaces Spring Boot's auto-configured {@code ClientRegistrationRepository}
 * entirely (Boot's {@code @ConditionalOnMissingBean} backs off once this bean
 * exists) so a {@code keycloak} registration built from database-stored
 * {@link KeycloakConfig} can sit alongside the two statically-configured
 * Google registrations.
 *
 * <p>{@code google-login}/{@code google-calendar} are served exactly as Boot
 * would have served them -- built once at startup via
 * {@link OAuth2ClientPropertiesMapper} (the same mapper Boot's own
 * auto-configuration uses internally), from {@code application.properties}.
 * Nothing about their resolution changes.
 *
 * <p>{@code keycloak} is built lazily from {@link KeycloakConfigService} on
 * first use, then cached -- OIDC issuer discovery is a network call, and
 * doing it on every login attempt would be wasteful. The cache is dropped by
 * {@link #onKeycloakConfigSaved} (fired synchronously by
 * {@link KeycloakConfigService#save}), so the next login attempt after an
 * Admin edit rebuilds it -- no restart needed. A discovery failure (bad
 * issuer URL, server unreachable) is swallowed here rather than thrown, so
 * {@link #findByRegistrationId} just returns {@code null} as if
 * unconfigured, instead of surfacing a raw discovery exception mid-login.
 */
@Component
class DynamicClientRegistrationRepository implements ClientRegistrationRepository {

    static final String KEYCLOAK_REGISTRATION_ID = "keycloak";

    private final Map<String, ClientRegistration> staticRegistrations;
    private final KeycloakConfigService keycloakConfigService;
    private final AtomicReference<ClientRegistration> keycloakRegistration = new AtomicReference<>();

    DynamicClientRegistrationRepository(OAuth2ClientProperties properties, KeycloakConfigService keycloakConfigService) {
        this.staticRegistrations = new OAuth2ClientPropertiesMapper(properties).asClientRegistrations();
        this.keycloakConfigService = keycloakConfigService;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (KEYCLOAK_REGISTRATION_ID.equals(registrationId)) {
            return resolveKeycloakRegistration();
        }
        return staticRegistrations.get(registrationId);
    }

    @EventListener
    void onKeycloakConfigSaved(KeycloakConfigSavedEvent event) {
        keycloakRegistration.set(null);
    }

    private ClientRegistration resolveKeycloakRegistration() {
        ClientRegistration cached = keycloakRegistration.get();
        if (cached != null) {
            return cached;
        }
        ClientRegistration built = buildKeycloakRegistration();
        if (built != null) {
            keycloakRegistration.compareAndSet(null, built);
        }
        return built;
    }

    private ClientRegistration buildKeycloakRegistration() {
        return keycloakConfigService.currentConfig()
                .map(this::toClientRegistration)
                .orElse(null);
    }

    // Package-private (not private) so tests can override it to stub out the
    // real network call OIDC discovery makes, while exercising the real
    // caching/invalidation logic in resolveKeycloakRegistration() untouched.
    ClientRegistration toClientRegistration(KeycloakConfig config) {
        String issuerUri = StringUtils.trimTrailingCharacter(config.getServerUrl(), '/')
                + "/realms/" + config.getRealm();
        try {
            return ClientRegistrations.fromIssuerLocation(issuerUri)
                    .registrationId(KEYCLOAK_REGISTRATION_ID)
                    .clientId(config.getClientId())
                    .clientSecret(config.getClientSecretEnc())
                    .build();
        } catch (RuntimeException discoveryFailed) {
            return null;
        }
    }
}
