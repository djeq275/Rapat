package com.example.vibe1.meeting;

/** Public seam the calendar module writes results back through. */
public interface MeetingSyncStatusPort {

    void markSynced(Long meetingId, String googleEventId);

    void markFailed(Long meetingId, String errorMessage);
}
