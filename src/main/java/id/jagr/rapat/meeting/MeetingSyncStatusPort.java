package id.jagr.rapat.meeting;

/** Public seam the calendar module writes results back through. */
public interface MeetingSyncStatusPort {

    void markSynced(Long meetingId, String googleEventId);

    void markFailed(Long meetingId, String errorMessage);
}
