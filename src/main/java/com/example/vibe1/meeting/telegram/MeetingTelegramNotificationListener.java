package com.example.vibe1.meeting.telegram;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.example.vibe1.meeting.MeetingScheduledEvent;
import com.example.vibe1.telegram.MeetingTelegramNotificationService;

import lombok.RequiredArgsConstructor;

/**
 * Lives inside the meeting module (not telegram.*): meeting -> telegram is
 * the allowed direction (the opposite of calendar's), so meeting is the side
 * that reaches out. The message itself was already composed and stored by
 * MeetingService when the meeting was created (see
 * MeetingTelegramNotificationService#recordSelection) -- this listener's only
 * job is to trigger sending whatever was recorded for this meeting.
 */
@Component
@RequiredArgsConstructor
class MeetingTelegramNotificationListener {

    private final MeetingTelegramNotificationService meetingTelegramNotificationService;

    @ApplicationModuleListener
    void on(MeetingScheduledEvent event) {
        meetingTelegramNotificationService.sendPendingNotifications(event.meetingId());
    }
}
