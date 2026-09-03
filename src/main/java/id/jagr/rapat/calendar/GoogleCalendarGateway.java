package id.jagr.rapat.calendar;

import id.jagr.rapat.meeting.MeetingSyncData;

/** Thin seam over the Google Calendar SDK so sync logic is mockable in tests. */
public interface GoogleCalendarGateway {

    /** @return the created Google Calendar event id. */
    String insertEvent(MeetingSyncData data);
}
