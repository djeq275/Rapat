package id.jagr.rapat.common;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;

import static org.assertj.core.api.Assertions.assertThat;

class AesGcmStringConverterTest {

    /** No concrete subclass is package-visible here; a minimal one is enough to exercise the shared logic. */
    static class TestConverter extends AesGcmStringConverter {
        TestConverter(String base64Key) {
            super(base64Key);
        }
    }

    private final AttributeConverter<String, String> converter =
            new TestConverter(Base64.getEncoder().encodeToString(new byte[32]));

    @Test
    void nullPassesThroughBothDirections() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void roundTripsPlainTextThroughEncryptionAndDecryption() {
        String plainText = "123456:AA-super-secret-bot-token";

        String stored = converter.convertToDatabaseColumn(plainText);

        assertThat(stored).isNotEqualTo(plainText);
        assertThat(converter.convertToEntityAttribute(stored)).isEqualTo(plainText);
    }

    @Test
    void sameInputEncryptsDifferentlyEachTime() {
        String plainText = "same-token";

        String first = converter.convertToDatabaseColumn(plainText);
        String second = converter.convertToDatabaseColumn(plainText);

        assertThat(first).isNotEqualTo(second);
    }
}
