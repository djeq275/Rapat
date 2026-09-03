package id.jagr.rapat.calendar;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import id.jagr.rapat.meeting.MeetingScheduledEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class MeetingCalendarSyncListener {

    private final CalendarSyncService calendarSyncService;

    @ApplicationModuleListener
    void on(MeetingScheduledEvent event) {
        calendarSyncService.sync(event.meetingId());
    }
}
