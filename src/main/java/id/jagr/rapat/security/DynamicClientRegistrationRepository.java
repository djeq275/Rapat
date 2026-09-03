package id.jagr.rapat.security;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.springframework.context.event.EventListener;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Replaces Spring Boot's auto-configured {@code ClientRegistrationRepository}
 * entirely (Boot's {@code @ConditionalOnMissingBean} backs off once this bean
 * exists) so every registration this app uses -- {@code keycloak},
 * {@code google-login}, {@code google-calendar} -- is built from
 * database-stored config instead of {@code application.properties}.
 *
 * <p>{@code keycloak} is built lazily from {@link KeycloakConfigService} via
 * OIDC issuer discovery (a network call, see {@link #toClientRegistration});
 * a discovery failure is swallowed to a {@code null} lookup rather than
 * thrown.
 *
 * <p>{@code google-login}/{@code google-calendar} are built lazily from
 * {@link GoogleOAuthConfigService} -- no discovery needed, since Google's
 * endpoints are already static via Spring Security's built-in
 * {@link CommonOAuth2Provider#GOOGLE}. Both share one {@link GoogleOAuthConfig}
 * client id/secret (one OAuth app in Google Cloud Console, same as before
 * this class existed) but differ in scope/grant-type/redirect-uri, matching
 * exactly what used to be declared per-registration in
 * {@code application.properties}.
 *
 * <p>Every registration is cached in memory after first build, since a
 * database read (and, for Keycloak, a discovery call) on every single
 * request would be wasteful. Each cache is dropped by its config's
 * {@code *ConfigSavedEvent} (fired synchronously by the owning
 * {@code *ConfigService.save}), so the next login/consent attempt after an
 * Admin edit rebuilds it -- no restart needed.
 */
@Component
class DynamicClientRegistrationRepository implements ClientRegistrationRepository {

    static final String KEYCLOAK_REGISTRATION_ID = "keycloak";
    static final String GOOGLE_LOGIN_REGISTRATION_ID = "google-login";
    static final String GOOGLE_CALENDAR_REGISTRATION_ID = "google-calendar";

    private final KeycloakConfigService keycloakConfigService;
    private final GoogleOAuthConfigService googleOAuthConfigService;

    private final AtomicReference<ClientRegistration> keycloakRegistration = new AtomicReference<>();
    private final AtomicReference<ClientRegistration> googleLoginRegistration = new AtomicReference<>();
    private final AtomicReference<ClientRegistration> googleCalendarRegistration = new AtomicReference<>();

    DynamicClientRegistrationRepository(KeycloakConfigService keycloakConfigService,
            GoogleOAuthConfigService googleOAuthConfigService) {
        this.keycloakConfigService = keycloakConfigService;
        this.googleOAuthConfigService = googleOAuthConfigService;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return switch (registrationId) {
            case KEYCLOAK_REGISTRATION_ID -> resolveCached(keycloakRegistration, this::buildKeycloakRegistration);
            case GOOGLE_LOGIN_REGISTRATION_ID -> resolveCached(googleLoginRegistration, this::buildGoogleLoginRegistration);
            case GOOGLE_CALENDAR_REGISTRATION_ID -> resolveCached(googleCalendarRegistration, this::buildGoogleCalendarRegistration);
            default -> null;
        };
    }

    @EventListener
    void onKeycloakConfigSaved(KeycloakConfigSavedEvent event) {
        keycloakRegistration.set(null);
    }

    @EventListener
    void onGoogleOAuthConfigSaved(GoogleOAuthConfigSavedEvent event) {
        googleLoginRegistration.set(null);
        googleCalendarRegistration.set(null);
    }

    private ClientRegistration resolveCached(AtomicReference<ClientRegistration> cache, Supplier<ClientRegistration> builder) {
        ClientRegistration cached = cache.get();
        if (cached != null) {
            return cached;
        }
        ClientRegistration built = builder.get();
        if (built != null) {
            cache.compareAndSet(null, built);
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
    // caching/invalidation logic in resolveCached() untouched.
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

    private ClientRegistration buildGoogleLoginRegistration() {
        return googleOAuthConfigService.currentConfig()
                .map(config -> CommonOAuth2Provider.GOOGLE.getBuilder(GOOGLE_LOGIN_REGISTRATION_ID)
                        .clientId(config.getClientId())
                        .clientSecret(config.getClientSecretEnc())
                        .scope("openid", "email", "profile")
                        .build())
                .orElse(null);
    }

    // Requested separately from google-login (incremental auth), only from
    // organizer-capable users -- see CalendarConsentStatus. Same OAuth client
    // as google-login, narrower scope, its own redirect-uri (must NOT be
    // under /login/oauth2/code/** -- that path is claimed by oauth2Login()'s
    // login filter, which would treat this callback as a login attempt
    // instead of a plain incremental-scope grant; see SecurityConfig).
    private ClientRegistration buildGoogleCalendarRegistration() {
        return googleOAuthConfigService.currentConfig()
                .map(config -> CommonOAuth2Provider.GOOGLE.getBuilder(GOOGLE_CALENDAR_REGISTRATION_ID)
                        .clientId(config.getClientId())
                        .clientSecret(config.getClientSecretEnc())
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .scope("https://www.googleapis.com/auth/calendar.events")
                        .redirectUri("{baseUrl}/connect/google-calendar")
                        .build())
                .orElse(null);
    }
}
