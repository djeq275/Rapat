package id.jagr.rapat.telegram;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Sends (and retries) meeting-invite notifications to Telegram groups.
 * Deliberately never depends on the meeting module: the message text is
 * composed by meeting.MeetingService (which has the meeting's content) and
 * handed in once at {@link #recordSelection}, then reused verbatim for both
 * the initial send and any later retry -- this module never fetches meeting
 * data itself.
 */
@Service
@RequiredArgsConstructor
public class MeetingTelegramNotificationService {

    private final MeetingTelegramGroupRepository meetingTelegramGroupRepository;
    private final TelegramGroupRepository telegramGroupRepository;
    private final TelegramGateway telegramGateway;

    @Transactional
    public void recordSelection(Long meetingId, List<Long> telegramGroupIds, String messageText) {
        for (Long groupId : telegramGroupIds) {
            if (meetingTelegramGroupRepository.existsByMeetingIdAndTelegramGroupId(meetingId, groupId)) {
                continue;
            }
            TelegramGroup group = telegramGroupRepository.findById(groupId)
                    .orElseThrow(() -> new IllegalArgumentException("Grup Telegram tidak ditemukan: " + groupId));
            MeetingTelegramGroup row = new MeetingTelegramGroup(meetingId, group);
            row.setMessageText(messageText);
            meetingTelegramGroupRepository.save(row);
        }
    }

    /** Sends every row recorded for this meeting -- called once, right after the meeting is created. */
    @Transactional
    public void sendPendingNotifications(Long meetingId) {
        for (MeetingTelegramGroup row : meetingTelegramGroupRepository.findByMeetingId(meetingId)) {
            sendOne(row);
        }
    }

    @Transactional
    public void retry(Long meetingId, Long telegramGroupId) {
        MeetingTelegramGroup row = meetingTelegramGroupRepository.findByMeetingIdAndTelegramGroupId(meetingId, telegramGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Data notifikasi Telegram tidak ditemukan"));
        sendOne(row);
    }

    @Transactional(readOnly = true)
    public List<MeetingTelegramGroup> findByMeetingId(Long meetingId) {
        return meetingTelegramGroupRepository.findByMeetingId(meetingId);
    }

    private void sendOne(MeetingTelegramGroup row) {
        try {
            telegramGateway.sendMessage(row.getTelegramGroup().getChatId(), row.getMessageText());
            row.setSendStatus(TelegramSendStatus.SENT);
            row.setSendError(null);
            row.setSentAt(Instant.now());
        } catch (Exception e) {
            // One group's failure must not stop the others in the same loop, nor the Calendar listener for this event.
            row.setSendStatus(TelegramSendStatus.FAILED);
            row.setSendError(e.getMessage());
        }
        meetingTelegramGroupRepository.save(row);
    }
}
