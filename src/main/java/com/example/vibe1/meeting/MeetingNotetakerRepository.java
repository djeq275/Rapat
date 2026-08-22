package com.example.vibe1.meeting;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingNotetakerRepository extends JpaRepository<MeetingNotetaker, Long> {

    List<MeetingNotetaker> findByMeetingId(Long meetingId);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);
}
