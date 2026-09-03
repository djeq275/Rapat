package id.jagr.rapat.telegram;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MeetingTelegramGroupRepository extends JpaRepository<MeetingTelegramGroup, Long> {

    @Query("select m from MeetingTelegramGroup m join fetch m.telegramGroup where m.meetingId = :meetingId")
    List<MeetingTelegramGroup> findByMeetingId(Long meetingId);

    @Query("select m from MeetingTelegramGroup m join fetch m.telegramGroup where m.meetingId = :meetingId and m.telegramGroup.id = :telegramGroupId")
    Optional<MeetingTelegramGroup> findByMeetingIdAndTelegramGroupId(Long meetingId, Long telegramGroupId);

    boolean existsByMeetingIdAndTelegramGroupId(Long meetingId, Long telegramGroupId);
}
