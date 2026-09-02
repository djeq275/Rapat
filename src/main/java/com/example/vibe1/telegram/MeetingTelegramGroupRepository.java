package com.example.vibe1.telegram;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MeetingTelegramGroupRepository extends JpaRepository<MeetingTelegramGroup, Long> {

    @Query("select m from MeetingTelegramGroup m join fetch m.telegramGroup where m.meetingId = :meetingId")
    List<MeetingTelegramGroup> findByMeetingId(Long meetingId);

    boolean existsByMeetingIdAndTelegramGroupId(Long meetingId, Long telegramGroupId);
}
