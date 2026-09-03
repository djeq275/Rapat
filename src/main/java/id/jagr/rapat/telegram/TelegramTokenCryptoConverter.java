package id.jagr.rapat.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import id.jagr.rapat.common.AesGcmStringConverter;

import jakarta.persistence.Converter;

/** Own encryption key, separate from calendar.TokenCryptoConverter's, so the two secret categories don't share a key. */
@Component
@Converter
public class TelegramTokenCryptoConverter extends AesGcmStringConverter {

    public TelegramTokenCryptoConverter(@Value("${telegram.token-encryption-key}") String base64Key) {
        super(base64Key);
    }
}
