package com.example.vibe1.telegram;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingTelegramNotificationServiceTest {

    @Mock
    MeetingTelegramGroupRepository meetingTelegramGroupRepository;
    @Mock
    TelegramGroupRepository telegramGroupRepository;
    @Mock
    TelegramGateway telegramGateway;

    MeetingTelegramNotificationService service;

    @Test
    void recordSelectionSkipsGroupsAlreadyRecordedForMeeting() {
        service = new MeetingTelegramNotificationService(meetingTelegramGroupRepository, telegramGroupRepository, telegramGateway);
        TelegramGroup group = new TelegramGroup("Divisi Engineering", "-100123");
        group.setId(2L);
        when(meetingTelegramGroupRepository.existsByMeetingIdAndTelegramGroupId(1L, 1L)).thenReturn(true);
        when(meetingTelegramGroupRepository.existsByMeetingIdAndTelegramGroupId(1L, 2L)).thenReturn(false);
        when(telegramGroupRepository.findById(2L)).thenReturn(Optional.of(group));
        when(meetingTelegramGroupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordSelection(1L, List.of(1L, 2L), "Undangan Rapat: Rapat Mingguan");

        ArgumentCaptor<MeetingTelegramGroup> captor = ArgumentCaptor.forClass(MeetingTelegramGroup.class);
        verify(meetingTelegramGroupRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getMeetingId()).isEqualTo(1L);
        assertThat(captor.getValue().getTelegramGroup()).isEqualTo(group);
        assertThat(captor.getValue().getMessageText()).isEqualTo("Undangan Rapat: Rapat Mingguan");
    }

    @Test
    void sendPendingNotificationsMarksEachRowSentOnGatewaySuccess() {
        service = new MeetingTelegramNotificationService(meetingTelegramGroupRepository, telegramGroupRepository, telegramGateway);
        TelegramGroup group = new TelegramGroup("Divisi Engineering", "-100123");
        group.setId(2L);
        MeetingTelegramGroup row = new MeetingTelegramGroup(1L, group);
        row.setMessageText("Undangan Rapat: Rapat Mingguan");
        when(meetingTelegramGroupRepository.findByMeetingId(1L)).thenReturn(List.of(row));
        lenient().when(meetingTelegramGroupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.sendPendingNotifications(1L);

        verify(telegramGateway).sendMessage("-100123", "Undangan Rapat: Rapat Mingguan");
        assertThat(row.getSendStatus()).isEqualTo(TelegramSendStatus.SENT);
        assertThat(row.getSendError()).isNull();
        assertThat(row.getSentAt()).isNotNull();
    }

    @Test
    void sendPendingNotificationsMarksRowFailedOnGatewayException() {
        service = new MeetingTelegramNotificationService(meetingTelegramGroupRepository, telegramGroupRepository, telegramGateway);
        TelegramGroup group = new TelegramGroup("Divisi Engineering", "-100123");
        group.setId(2L);
        MeetingTelegramGroup row = new MeetingTelegramGroup(1L, group);
        row.setMessageText("Undangan Rapat: Rapat Mingguan");
        when(meetingTelegramGroupRepository.findByMeetingId(1L)).thenReturn(List.of(row));
        lenient().when(meetingTelegramGroupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new TelegramSendException("chat not found"))
                .when(telegramGateway).sendMessage(eq("-100123"), any());

        service.sendPendingNotifications(1L);

        assertThat(row.getSendStatus()).isEqualTo(TelegramSendStatus.FAILED);
        assertThat(row.getSendError()).isEqualTo("chat not found");
    }

    @Test
    void retrySendsOnlyTheRequestedMeetingGroupPair() {
        service = new MeetingTelegramNotificationService(meetingTelegramGroupRepository, telegramGroupRepository, telegramGateway);
        TelegramGroup group = new TelegramGroup("Divisi Engineering", "-100123");
        group.setId(2L);
        MeetingTelegramGroup row = new MeetingTelegramGroup(1L, group);
        row.setMessageText("Undangan Rapat: Rapat Mingguan");
        row.setSendStatus(TelegramSendStatus.FAILED);
        row.setSendError("previous error");
        when(meetingTelegramGroupRepository.findByMeetingIdAndTelegramGroupId(1L, 2L)).thenReturn(Optional.of(row));
        lenient().when(meetingTelegramGroupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.retry(1L, 2L);

        verify(telegramGateway).sendMessage("-100123", "Undangan Rapat: Rapat Mingguan");
        assertThat(row.getSendStatus()).isEqualTo(TelegramSendStatus.SENT);
        verify(meetingTelegramGroupRepository, never()).findByMeetingId(any());
    }
}
