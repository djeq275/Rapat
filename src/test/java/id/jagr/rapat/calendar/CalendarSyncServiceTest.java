package id.jagr.rapat.calendar;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import id.jagr.rapat.meeting.MeetingSyncData;
import id.jagr.rapat.meeting.MeetingSyncQueryPort;
import id.jagr.rapat.meeting.MeetingSyncStatusPort;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarSyncServiceTest {

    @Mock
    MeetingSyncQueryPort meetingSyncQueryPort;
    @Mock
    MeetingSyncStatusPort meetingSyncStatusPort;
    @Mock
    GoogleCalendarGateway calendarGateway;

    CalendarSyncService service;

    private MeetingSyncData sampleData() {
        Instant now = Instant.now();
        return new MeetingSyncData(1L, "Rapat", null, null, now, now.plusSeconds(3600),
                "ketua@company.local", List.of("karyawan@company.local"));
    }

    @Test
    void successMarksSynced() {
        service = new CalendarSyncService(meetingSyncQueryPort, meetingSyncStatusPort, calendarGateway);
        when(meetingSyncQueryPort.loadForSync(1L)).thenReturn(sampleData());
        when(calendarGateway.insertEvent(any())).thenReturn("google-event-123");

        service.sync(1L);

        verify(meetingSyncStatusPort).markSynced(1L, "google-event-123");
        verify(meetingSyncStatusPort, never()).markFailed(any(), anyString());
    }

    @Test
    void failureMarksFailedWithoutRethrowing() {
        service = new CalendarSyncService(meetingSyncQueryPort, meetingSyncStatusPort, calendarGateway);
        when(meetingSyncQueryPort.loadForSync(1L)).thenReturn(sampleData());
        when(calendarGateway.insertEvent(any())).thenThrow(new CalendarSyncException("token expired"));

        service.sync(1L);

        verify(meetingSyncStatusPort).markFailed(1L, "token expired");
        verify(meetingSyncStatusPort, never()).markSynced(any(), anyString());
    }
}
