package id.jagr.rapat.security;

/**
 * Published right after {@link KeycloakConfigService#save} commits, so
 * {@link DynamicClientRegistrationRepository} can drop its cached
 * {@code keycloak} {@code ClientRegistration} and rebuild it (lazily, on the
 * next login attempt) from the new config -- no restart needed.
 *
 * <p>Plain Spring {@code ApplicationEventPublisher}, not Modulith's durable
 * event publication registry: this never leaves the {@code security}
 * module, needs synchronous same-thread delivery (so the cache is already
 * cleared by the time {@code save} returns), and a missed invalidation
 * carries no data-loss risk worth the durability machinery.
 */
record KeycloakConfigSavedEvent() {
}
