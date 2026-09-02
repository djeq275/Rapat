package com.example.vibe1.telegram;

import java.time.Instant;

import com.example.vibe1.common.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One Telegram group selected for one meeting, plus its own send status --
 * unlike Meeting.calendarSyncStatus (one status for the whole meeting, since
 * there's only one Calendar event), a meeting can succeed to group A and fail
 * to group B, so status lives per (meeting, group) row here.
 *
 * <p>{@code meetingId} is a plain column, not {@code @ManyToOne Meeting}: the
 * telegram module must never depend on meeting (meeting depends on telegram
 * instead, one-way -- see package-info/CLAUDE.md for why the direction is
 * flipped compared to the calendar module).
 */
@Entity
@Table(name = "meeting_telegram_group",
        uniqueConstraints = @UniqueConstraint(name = "uk_meeting_telegram_group", columnNames = {"meeting_id", "telegram_group_id"}))
@Getter
@Setter
@NoArgsConstructor
public class MeetingTelegramGroup extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "telegram_group_id", nullable = false)
    private TelegramGroup telegramGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status", nullable = false, length = 16)
    private TelegramSendStatus sendStatus = TelegramSendStatus.PENDING;

    @Lob
    @Column(name = "send_error", columnDefinition = "LONGTEXT")
    private String sendError;

    @Column(name = "sent_at")
    private Instant sentAt;

    public MeetingTelegramGroup(Long meetingId, TelegramGroup telegramGroup) {
        this.meetingId = meetingId;
        this.telegramGroup = telegramGroup;
    }
}
