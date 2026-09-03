package id.jagr.rapat.calendar;

import java.time.Instant;

import id.jagr.rapat.common.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Durable, encrypted-at-rest store for a user's Google OAuth tokens for one
 * client registration (in practice, only "google-calendar" -- google-login
 * stays session-scoped). Keyed by principal name (the User's email), not a
 * User FK, so this module never depends on the user module.
 */
@Entity
@Table(name = "google_authorized_client",
        uniqueConstraints = @UniqueConstraint(name = "uk_google_authorized_client",
                columnNames = {"principal_name", "client_registration_id"}))
@Getter
@Setter
@NoArgsConstructor
public class GoogleAuthorizedClient extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "principal_name", nullable = false)
    private String principalName;

    @Column(name = "client_registration_id", nullable = false)
    private String clientRegistrationId;

    @Lob
    @Convert(converter = TokenCryptoConverter.class)
    @Column(name = "access_token_enc", nullable = false, columnDefinition = "LONGTEXT")
    private String accessTokenEnc;

    @Column(name = "access_token_issued_at", nullable = false)
    private Instant accessTokenIssuedAt;

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    @Lob
    @Convert(converter = TokenCryptoConverter.class)
    @Column(name = "refresh_token_enc", columnDefinition = "LONGTEXT")
    private String refreshTokenEnc;

    /** Comma-separated. */
    @Column(name = "scopes")
    private String scopes;
}
