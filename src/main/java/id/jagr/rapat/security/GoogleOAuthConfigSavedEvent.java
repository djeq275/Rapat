package id.jagr.rapat.security;

/**
 * Published right after {@link GoogleOAuthConfigService#save} commits, so
 * {@link DynamicClientRegistrationRepository} can drop its cached
 * {@code google-login}/{@code google-calendar} {@code ClientRegistration}s
 * and rebuild them (lazily, on the next login/consent attempt) from the new
 * config -- no restart needed. Mirrors {@link KeycloakConfigSavedEvent}'s
 * rationale for using a plain Spring event rather than Modulith's durable
 * registry.
 */
record GoogleOAuthConfigSavedEvent() {
}
