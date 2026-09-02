package com.example.vibe1.telegram;

import com.example.vibe1.division.Division;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A division's favorite Telegram group -- pre-selected by default on the
 * meeting-creation form, but not a restriction (Ketua Divisi can still pick
 * other active groups). {@code @ManyToOne Division} is safe here: division is
 * a leaf module with no outgoing dependencies, so telegram -> division can't
 * create a cycle the way telegram -> meeting would (see MeetingTelegramGroup).
 */
@Entity
@Table(name = "division_telegram_group",
        uniqueConstraints = @UniqueConstraint(name = "uk_division_telegram_group", columnNames = {"division_id", "telegram_group_id"}))
@Getter
@Setter
@NoArgsConstructor
public class DivisionTelegramGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "division_id", nullable = false)
    private Division division;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "telegram_group_id", nullable = false)
    private TelegramGroup telegramGroup;

    public DivisionTelegramGroup(Division division, TelegramGroup telegramGroup) {
        this.division = division;
        this.telegramGroup = telegramGroup;
    }
}
