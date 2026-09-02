package com.example.vibe1.security;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Keeps exactly one {@link KeycloakConfig} row -- reads/replaces it, never
 * inserts a second one. The Admin form (issue #25) calls {@link #save} to
 * set/update the connection details; issue #26's dynamic
 * {@code ClientRegistrationRepository} calls {@link #currentConfig} to build
 * the {@code keycloak} registration and must invalidate its cache whenever
 * {@link #save} runs.
 */
@Service
@RequiredArgsConstructor
public class KeycloakConfigService {

    private final KeycloakConfigRepository repository;

    @Transactional(readOnly = true)
    public Optional<KeycloakConfig> currentConfig() {
        return repository.findFirstByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public boolean isConfigured() {
        return repository.findFirstByOrderByIdAsc().isPresent();
    }

    /**
     * {@code clientSecret} blank means "keep the existing secret unchanged" --
     * same convention as {@code UserForm.password} / {@code TelegramSettingsForm.token}.
     * Required on first save, since the column itself is non-nullable.
     */
    @Transactional
    public void save(String serverUrl, String realm, String clientId, String clientSecret) {
        Optional<KeycloakConfig> existing = repository.findFirstByOrderByIdAsc();
        KeycloakConfig config = existing.orElseGet(KeycloakConfig::new);
        config.setServerUrl(serverUrl);
        config.setRealm(realm);
        config.setClientId(clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            config.setClientSecretEnc(clientSecret);
        } else if (existing.isEmpty()) {
            throw new IllegalArgumentException("Client secret wajib diisi saat pertama kali mengatur konfigurasi Keycloak");
        }
        repository.save(config);
    }
}
