package com.example.vibe1.meeting;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByDivisionIdOrderByStartTimeDesc(Long divisionId);

    List<Meeting> findAllByOrderByStartTimeDesc();
}
