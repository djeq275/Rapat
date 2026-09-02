package com.example.vibe1.security;

import com.example.vibe1.common.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Single-row config for the app's one Keycloak SSO connection, mirroring
 * {@code telegram.TelegramBotConfig}'s shape. Admin sets/replaces it via a
 * form (see issue #25) -- the client secret is never displayed back once
 * saved.
 *
 * <p>{@code serverUrl} and {@code realm} are stored separately (not as one
 * pre-combined issuer URI) so the OIDC issuer path (typically
 * {@code {serverUrl}/realms/{realm}}) can be derived when building the
 * {@code ClientRegistration} (see issue #26), rather than risking the two
 * pieces drifting out of sync if stored redundantly.
 */
@Entity
@Table(name = "keycloak_config")
@Getter
@Setter
@NoArgsConstructor
public class KeycloakConfig extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_url", nullable = false)
    private String serverUrl;

    @Column(nullable = false)
    private String realm;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Lob
    @Convert(converter = KeycloakTokenCryptoConverter.class)
    @Column(name = "client_secret_enc", nullable = false, columnDefinition = "LONGTEXT")
    private String clientSecretEnc;
}
