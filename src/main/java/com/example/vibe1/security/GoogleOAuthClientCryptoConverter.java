package com.example.vibe1.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.vibe1.common.AesGcmStringConverter;

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
