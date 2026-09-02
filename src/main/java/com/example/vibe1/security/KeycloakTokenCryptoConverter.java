package com.example.vibe1.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.vibe1.common.AesGcmStringConverter;

import jakarta.persistence.Converter;

/** Own encryption key, separate from calendar.TokenCryptoConverter's and telegram.TelegramTokenCryptoConverter's, so the secret categories don't share a key. */
@Component
@Converter
public class KeycloakTokenCryptoConverter extends AesGcmStringConverter {

    public KeycloakTokenCryptoConverter(@Value("${keycloak.token-encryption-key}") String base64Key) {
        super(base64Key);
    }
}
