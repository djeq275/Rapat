package com.example.vibe1.calendar;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.example.vibe1.meeting.MeetingScheduledEvent;

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
