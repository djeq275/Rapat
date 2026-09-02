package com.example.vibe1.telegram;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.vibe1.division.Division;
import com.example.vibe1.division.DivisionRepository;

import lombok.RequiredArgsConstructor;

/** Which Telegram groups are a division's favorites -- default selection on the meeting-creation form (see issue #10). */
@Service
@RequiredArgsConstructor
public class DivisionTelegramGroupService {

    private final DivisionTelegramGroupRepository divisionTelegramGroupRepository;
    private final TelegramGroupRepository telegramGroupRepository;
    private final DivisionRepository divisionRepository;

    @Transactional(readOnly = true)
    public Set<Long> findFavoriteGroupIds(Long divisionId) {
        return divisionTelegramGroupRepository.findByDivisionId(divisionId).stream()
                .map(favorite -> favorite.getTelegramGroup().getId())
                .collect(Collectors.toSet());
    }

    /** Replaces the whole favorite set for a division -- simplest correct approach for a small admin-managed list. */
    @Transactional
    public void replaceFavorites(Long divisionId, List<Long> telegramGroupIds) {
        Division division = divisionRepository.findById(divisionId)
                .orElseThrow(() -> new IllegalArgumentException("Divisi tidak ditemukan"));

        divisionTelegramGroupRepository.deleteAll(divisionTelegramGroupRepository.findByDivisionId(divisionId));

        for (Long groupId : telegramGroupIds) {
            TelegramGroup group = telegramGroupRepository.findById(groupId)
                    .orElseThrow(() -> new IllegalArgumentException("Grup Telegram tidak ditemukan: " + groupId));
            divisionTelegramGroupRepository.save(new DivisionTelegramGroup(division, group));
        }
    }
}
