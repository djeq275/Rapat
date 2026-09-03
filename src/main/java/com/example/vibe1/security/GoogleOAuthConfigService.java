package com.example.vibe1.security;

import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Keeps exactly one {@link GoogleOAuthConfig} row -- reads/replaces it,
 * never inserts a second one. The Admin form (issue #31) calls {@link #save}
 * to set/update the client id/secret; {@link #save} then publishes
 * {@link GoogleOAuthConfigSavedEvent} so {@link DynamicClientRegistrationRepository}
 * invalidates its cached {@code google-login}/{@code google-calendar}
 * registrations -- the next login/consent attempt picks up the new config
 * without an app restart. {@link GoogleOAuthConfigBootstrap} also calls
 * {@link #save} once, on first startup, to seed this table from `.env`.
 */
@Service
@RequiredArgsConstructor
public class GoogleOAuthConfigService {

    private final GoogleOAuthConfigRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Optional<GoogleOAuthConfig> currentConfig() {
        return repository.findFirstByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public boolean isConfigured() {
        return repository.findFirstByOrderByIdAsc().isPresent();
    }

    /**
     * {@code clientSecret} blank means "keep the existing secret unchanged" --
     * same convention as {@code UserForm.password} / {@code KeycloakSettingsForm.clientSecret}.
     * Required on first save, since the column itself is non-nullable.
     */
    @Transactional
    public void save(String clientId, String clientSecret) {
        Optional<GoogleOAuthConfig> existing = repository.findFirstByOrderByIdAsc();
        GoogleOAuthConfig config = existing.orElseGet(GoogleOAuthConfig::new);
        config.setClientId(clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            config.setClientSecretEnc(clientSecret);
        } else if (existing.isEmpty()) {
            throw new IllegalArgumentException("Client secret wajib diisi saat pertama kali mengatur konfigurasi Google OAuth");
        }
        repository.save(config);
        eventPublisher.publishEvent(new GoogleOAuthConfigSavedEvent());
    }
}
