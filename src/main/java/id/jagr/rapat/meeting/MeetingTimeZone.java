package id.jagr.rapat.meeting;

import java.time.ZoneId;

/** Single company, single timezone -- see PRD non-goals (no multi-tenant). */
public final class MeetingTimeZone {

    public static final ZoneId WIB = ZoneId.of("Asia/Jakarta");

    private MeetingTimeZone() {
    }
}
