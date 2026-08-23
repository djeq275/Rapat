package com.example.vibe1.meeting;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MeetingNotetakerRepository extends JpaRepository<MeetingNotetaker, Long> {

    @Query("select n from MeetingNotetaker n join fetch n.user where n.meeting.id = :meetingId")
    List<MeetingNotetaker> findByMeetingId(Long meetingId);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    Optional<MeetingNotetaker> findByMeetingIdAndUserId(Long meetingId, Long userId);
}
