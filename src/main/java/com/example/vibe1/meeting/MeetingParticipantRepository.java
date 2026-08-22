package com.example.vibe1.meeting;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    @Query("select p from MeetingParticipant p join fetch p.user where p.meeting.id = :meetingId")
    List<MeetingParticipant> findByMeetingId(Long meetingId);

    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);
}
