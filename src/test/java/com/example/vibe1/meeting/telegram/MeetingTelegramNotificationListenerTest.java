package com.example.vibe1.meeting.telegram;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.vibe1.meeting.MeetingScheduledEvent;
import com.example.vibe1.telegram.MeetingTelegramNotificationService;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MeetingTelegramNotificationListenerTest {

    @Mock
    MeetingTelegramNotificationService meetingTelegramNotificationService;

    @Test
    void triggersSendingWhateverWasRecordedForTheScheduledMeeting() {
        MeetingTelegramNotificationListener listener = new MeetingTelegramNotificationListener(meetingTelegramNotificationService);

        listener.on(new MeetingScheduledEvent(42L));

        verify(meetingTelegramNotificationService).sendPendingNotifications(42L);
    }
}
