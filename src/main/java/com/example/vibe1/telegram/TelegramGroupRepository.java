package com.example.vibe1.telegram;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramGroupRepository extends JpaRepository<TelegramGroup, Long> {

    List<TelegramGroup> findByEnabledTrue();

    Optional<TelegramGroup> findByChatId(String chatId);
}
