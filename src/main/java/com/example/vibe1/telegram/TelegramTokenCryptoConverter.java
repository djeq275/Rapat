package com.example.vibe1.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.vibe1.common.AesGcmStringConverter;

import jakarta.persistence.Converter;

/** Own encryption key, separate from calendar.TokenCryptoConverter's, so the two secret categories don't share a key. */
@Component
@Converter
public class TelegramTokenCryptoConverter extends AesGcmStringConverter {

    public TelegramTokenCryptoConverter(@Value("${telegram.token-encryption-key}") String base64Key) {
        super(base64Key);
    }
}
