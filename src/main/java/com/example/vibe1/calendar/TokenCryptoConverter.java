package com.example.vibe1.calendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.vibe1.common.AesGcmStringConverter;

import jakarta.persistence.Converter;

/**
 * AES-256-GCM at rest for OAuth access/refresh tokens (PRD requires encrypted
 * storage, and Spring's own JdbcOAuth2AuthorizedClientService stores them
 * plaintext with no hook for this). Registered as a Spring bean so Hibernate's
 * Spring-aware BeanContainer injects the key instead of us hand-rolling a
 * static holder.
 */
@Component
@Converter
public class TokenCryptoConverter extends AesGcmStringConverter {

    public TokenCryptoConverter(@Value("${google.token-encryption-key}") String base64Key) {
        super(base64Key);
    }
}
