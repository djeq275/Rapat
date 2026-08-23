package com.example.vibe1.meeting;

import com.example.vibe1.user.User;

/** Lets both the meeting detail view and the calendar module's retry action share one rule. */
public interface MeetingSyncAuthorizationPort {

    boolean canRetrySync(User user, Long meetingId);

    void assertCanRetrySync(User user, Long meetingId);
}
