package com.example.vibe1.telegram;

import com.example.vibe1.common.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A Telegram group the app can send meeting invites to. Never hard-deleted --
 * {@link #enabled} is turned off instead, so meetings that already sent to a
 * retired group keep their history (see {@link MeetingTelegramGroup}).
 */
@Entity
@Table(name = "telegram_group")
@Getter
@Setter
@NoArgsConstructor
public class TelegramGroup extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "chat_id", nullable = false, unique = true)
    private String chatId;

    @Column(nullable = false)
    private boolean enabled = true;

    public TelegramGroup(String name, String chatId) {
        this.name = name;
        this.chatId = chatId;
    }
}
