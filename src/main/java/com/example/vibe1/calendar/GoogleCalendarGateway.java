package com.example.vibe1.calendar;

import com.example.vibe1.meeting.MeetingSyncData;

/** Thin seam over the Google Calendar SDK so sync logic is mockable in tests. */
public interface GoogleCalendarGateway {

    /** @return the created Google Calendar event id. */
    String insertEvent(MeetingSyncData data);
}
