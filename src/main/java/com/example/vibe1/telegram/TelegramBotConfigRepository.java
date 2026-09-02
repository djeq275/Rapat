package com.example.vibe1.telegram;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface TelegramBotConfigRepository extends JpaRepository<TelegramBotConfig, Long> {

    Optional<TelegramBotConfig> findFirstByOrderByIdAsc();
}
