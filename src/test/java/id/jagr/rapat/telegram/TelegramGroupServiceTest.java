package id.jagr.rapat.telegram;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramGroupServiceTest {

    @Mock
    TelegramGroupRepository telegramGroupRepository;

    TelegramGroupService service;

    @Test
    void rejectsDuplicateChatIdOnCreate() {
        service = new TelegramGroupService(telegramGroupRepository);
        TelegramGroup existing = new TelegramGroup("Divisi Lain", "-1001111111111");
        existing.setId(1L);
        when(telegramGroupRepository.findByChatId("-1001111111111")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create("Grup Baru", "-1001111111111"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsCreatingWithUnusedChatId() {
        service = new TelegramGroupService(telegramGroupRepository);
        lenient().when(telegramGroupRepository.findByChatId(any())).thenReturn(Optional.empty());
        when(telegramGroupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> service.create("Grup Baru", "-1002222222222")).doesNotThrowAnyException();
    }

    @Test
    void updateAllowsKeepingItsOwnChatId() {
        service = new TelegramGroupService(telegramGroupRepository);
        TelegramGroup group = new TelegramGroup("Grup A", "-1003333333333");
        group.setId(5L);
        when(telegramGroupRepository.findByChatId("-1003333333333")).thenReturn(Optional.of(group));
        when(telegramGroupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(telegramGroupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> service.update(5L, "Grup A (renamed)", "-1003333333333", true))
                .doesNotThrowAnyException();
    }
}
