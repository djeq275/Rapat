package com.example.vibe1.calendar;

import org.springframework.stereotype.Service;

import com.example.vibe1.meeting.MeetingSyncData;
import com.example.vibe1.meeting.MeetingSyncQueryPort;
import com.example.vibe1.meeting.MeetingSyncStatusPort;

import lombok.RequiredArgsConstructor;

/** Shared by the automatic listener and the manual "retry sync" action. */
@Service
@RequiredArgsConstructor
public class CalendarSyncService {

    private final MeetingSyncQueryPort meetingSyncQueryPort;
    private final MeetingSyncStatusPort meetingSyncStatusPort;
    private final GoogleCalendarGateway calendarGateway;

    public void sync(Long meetingId) {
        MeetingSyncData data = meetingSyncQueryPort.loadForSync(meetingId);
        try {
            String googleEventId = calendarGateway.insertEvent(data);
            meetingSyncStatusPort.markSynced(meetingId, googleEventId);
        } catch (Exception ex) {
            meetingSyncStatusPort.markFailed(meetingId, ex.getMessage());
        }
    }
}
