package com.example.vibe1.meeting;

import java.time.Instant;
import java.util.List;

public record CreateMeetingCommand(
        String title,
        String description,
        String materialLink,
        Instant startTime,
        Instant endTime,
        Long divisionId,
        Long organizerId,
        List<Long> participantUserIds) {
}
