package com.example.vibe1.meeting;

import java.time.Instant;
import java.util.List;

/** Read-only projection the calendar module needs to create a Google Calendar event. */
public record MeetingSyncData(
        Long meetingId,
        String title,
        String description,
        String materialLink,
        Instant startTime,
        Instant endTime,
        String organizerEmail,
        List<String> participantEmails) {
}
