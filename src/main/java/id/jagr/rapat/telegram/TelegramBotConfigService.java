package id.jagr.rapat.telegram;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Keeps exactly one {@link TelegramBotConfig} row -- reads/replaces it,
 * never inserts a second one. The Admin form (issue #9) calls {@link #save}
 * to set/replace the token; {@link TelegramGateway} calls {@link #currentToken}
 * to send messages.
 */
@Service
@RequiredArgsConstructor
public class TelegramBotConfigService {

    private final TelegramBotConfigRepository repository;

    @Transactional(readOnly = true)
    public Optional<String> currentToken() {
        return repository.findFirstByOrderByIdAsc().map(TelegramBotConfig::getBotTokenEnc);
    }

    @Transactional
    public void save(String token) {
        TelegramBotConfig config = repository.findFirstByOrderByIdAsc().orElseGet(TelegramBotConfig::new);
        config.setBotTokenEnc(token);
        repository.save(config);
    }
}
