package com.example.vibe1.meeting;

/** Public seam the calendar module reads through -- it never touches Meeting/MeetingService directly. */
public interface MeetingSyncQueryPort {

    MeetingSyncData loadForSync(Long meetingId);
}
