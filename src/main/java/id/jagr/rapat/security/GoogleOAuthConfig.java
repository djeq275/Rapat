package id.jagr.rapat.security;

import id.jagr.rapat.common.AuditableEntity;

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
 * Single-row config for the app's one Google OAuth client, mirroring
 * {@link KeycloakConfig}'s shape. Admin sets/replaces it via a form (see
 * issue #31) -- the client secret is never displayed back once saved.
 *
 * <p>No issuer/realm/server-URL fields like {@code KeycloakConfig}: Google's
 * endpoints are already static via Spring Security's built-in
 * {@code CommonOAuth2Provider.GOOGLE}, so there's nothing to discover --
 * just the client id/secret this app's OAuth app was issued.
 *
 * <p>Not to be confused with {@code calendar.GoogleAuthorizedClientService}'s
 * stored access/refresh tokens: those are per-user tokens issued *through*
 * this client after a successful login/consent; this row is the client
 * itself, one per app installation, shared by both the {@code google-login}
 * and {@code google-calendar} registrations (see issue #29/#32).
 */
@Entity
@Table(name = "google_oauth_config")
@Getter
@Setter
@NoArgsConstructor
public class GoogleOAuthConfig extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Lob
    @Convert(converter = GoogleOAuthClientCryptoConverter.class)
    @Column(name = "client_secret_enc", nullable = false, columnDefinition = "LONGTEXT")
    private String clientSecretEnc;
}
