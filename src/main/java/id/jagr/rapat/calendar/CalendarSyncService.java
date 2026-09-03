package id.jagr.rapat.calendar;

import org.springframework.stereotype.Service;

import id.jagr.rapat.meeting.MeetingSyncData;
import id.jagr.rapat.meeting.MeetingSyncQueryPort;
import id.jagr.rapat.meeting.MeetingSyncStatusPort;

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
