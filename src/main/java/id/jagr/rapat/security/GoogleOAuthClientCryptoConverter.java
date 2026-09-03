package id.jagr.rapat.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import id.jagr.rapat.common.AesGcmStringConverter;

import jakarta.persistence.Converter;

/**
 * Own encryption key, separate from every other secret category in the app --
 * notably including {@code calendar.TokenCryptoConverter}'s
 * {@code GOOGLE_TOKEN_ENCRYPTION_KEY}, which is a *different* Google secret
 * category (per-user OAuth access/refresh tokens, not this OAuth client's
 * own secret). Also separate from Keycloak's and Telegram's keys.
 */
@Component
@Converter
public class GoogleOAuthClientCryptoConverter extends AesGcmStringConverter {

    public GoogleOAuthClientCryptoConverter(@Value("${google.oauth-client-encryption-key}") String base64Key) {
        super(base64Key);
    }
}
