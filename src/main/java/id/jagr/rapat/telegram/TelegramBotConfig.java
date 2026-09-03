package id.jagr.rapat.telegram;

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
 * Single-row config holding the app's one Telegram bot token, encrypted at
 * rest. Admin sets/replaces it via a form (see issue #9) -- never displayed
 * back as plaintext once saved.
 */
@Entity
@Table(name = "telegram_bot_config")
@Getter
@Setter
@NoArgsConstructor
public class TelegramBotConfig extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Convert(converter = TelegramTokenCryptoConverter.class)
    @Column(name = "bot_token_enc", nullable = false, columnDefinition = "LONGTEXT")
    private String botTokenEnc;
}
