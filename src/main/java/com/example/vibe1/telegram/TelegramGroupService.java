package com.example.vibe1.telegram;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelegramGroupService {

    private final TelegramGroupRepository telegramGroupRepository;

    public List<TelegramGroup> findAll() {
        return telegramGroupRepository.findAll();
    }

    public List<TelegramGroup> findActive() {
        return telegramGroupRepository.findByEnabledTrue();
    }

    public TelegramGroup findById(Long id) {
        return telegramGroupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Grup Telegram tidak ditemukan"));
    }

    @Transactional
    public TelegramGroup create(String name, String chatId) {
        assertChatIdAvailable(chatId, null);
        return telegramGroupRepository.save(new TelegramGroup(name, chatId));
    }

    @Transactional
    public TelegramGroup update(Long id, String name, String chatId, boolean enabled) {
        assertChatIdAvailable(chatId, id);
        TelegramGroup group = findById(id);
        group.setName(name);
        group.setChatId(chatId);
        group.setEnabled(enabled);
        return telegramGroupRepository.save(group);
    }

    private void assertChatIdAvailable(String chatId, Long excludingId) {
        telegramGroupRepository.findByChatId(chatId)
                .filter(existing -> !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Chat ID sudah dipakai grup lain");
                });
    }
}
