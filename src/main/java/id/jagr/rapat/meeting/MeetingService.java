package id.jagr.rapat.meeting;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.jagr.rapat.division.Division;
import id.jagr.rapat.division.DivisionRepository;
import id.jagr.rapat.telegram.MeetingTelegramNotificationService;
import id.jagr.rapat.user.Role;
import id.jagr.rapat.user.User;
import id.jagr.rapat.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetingService implements MeetingSyncQueryPort, MeetingSyncStatusPort, MeetingSyncAuthorizationPort {

    private static final DateTimeFormatter MESSAGE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.of("id", "ID"));

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final DivisionRepository divisionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MeetingTelegramNotificationService meetingTelegramNotificationService;

    @Transactional
    public Meeting create(CreateMeetingCommand command) {
        Division division = divisionRepository.findById(command.divisionId())
                .orElseThrow(() -> new IllegalArgumentException("Divisi tidak ditemukan"));
        User organizer = userRepository.findById(command.organizerId())
                .orElseThrow(() -> new IllegalArgumentException("Organizer tidak ditemukan"));

        if (organizer.getRole() != Role.KETUA_DIVISI
                || organizer.getDivision() == null
                || !organizer.getDivision().getId().equals(division.getId())) {
            throw new IllegalArgumentException("Hanya Ketua Divisi dari divisi tersebut yang bisa membuat rapat ini");
        }
        if (command.endTime().isBefore(command.startTime()) || command.endTime().equals(command.startTime())) {
            throw new IllegalArgumentException("Waktu selesai harus setelah waktu mulai");
        }

        Meeting meeting = new Meeting();
        meeting.setTitle(command.title());
        meeting.setDescription(command.description());
        meeting.setMaterialLink(command.materialLink());
        meeting.setStartTime(command.startTime());
        meeting.setEndTime(command.endTime());
        meeting.setDivision(division);
        meeting.setOrganizer(organizer);
        meeting.setCalendarSyncStatus(CalendarSyncStatus.PENDING);
        meeting = meetingRepository.save(meeting);

        for (Long userId : command.participantUserIds()) {
            User participant = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Peserta tidak ditemukan: " + userId));
            if (participant.getDivision() == null || !participant.getDivision().getId().equals(division.getId())) {
                throw new IllegalArgumentException("Peserta harus dari divisi yang sama dengan rapat");
            }
            addParticipant(meeting, participant, ParticipantAddedReason.SELECTED);
        }

        for (User direktur : userRepository.findByRole(Role.DIREKTUR)) {
            addParticipant(meeting, direktur, ParticipantAddedReason.DIREKTUR_AUTO);
        }

        if (!command.telegramGroupIds().isEmpty()) {
            meetingTelegramNotificationService.recordSelection(
                    meeting.getId(), command.telegramGroupIds(), buildTelegramMessage(meeting));
        }

        eventPublisher.publishEvent(new MeetingScheduledEvent(meeting.getId()));
        return meeting;
    }

    private String buildTelegramMessage(Meeting meeting) {
        StringBuilder message = new StringBuilder()
                .append("Undangan Rapat: ").append(meeting.getTitle()).append('\n')
                .append("Waktu: ").append(MESSAGE_TIME_FORMAT.format(meeting.getStartTimeLocal()))
                .append(" - ").append(MESSAGE_TIME_FORMAT.format(meeting.getEndTimeLocal()));
        if (meeting.getMaterialLink() != null) {
            message.append("\nMateri: ").append(meeting.getMaterialLink());
        }
        return message.toString();
    }

    private void addParticipant(Meeting meeting, User user, ParticipantAddedReason reason) {
        if (participantRepository.existsByMeetingIdAndUserId(meeting.getId(), user.getId())) {
            return;
        }
        participantRepository.save(new MeetingParticipant(meeting, user, reason));
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingSyncData loadForSync(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Rapat tidak ditemukan: " + meetingId));
        List<String> participantEmails = participantRepository.findByMeetingId(meetingId).stream()
                .map(p -> p.getUser().getEmail())
                .toList();

        return new MeetingSyncData(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getMaterialLink(),
                meeting.getStartTime(),
                meeting.getEndTime(),
                meeting.getOrganizer().getEmail(),
                participantEmails);
    }

    @Override
    @Transactional
    public void markSynced(Long meetingId, String googleEventId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Rapat tidak ditemukan: " + meetingId));
        meeting.setGoogleEventId(googleEventId);
        meeting.setCalendarSyncStatus(CalendarSyncStatus.SYNCED);
        meeting.setCalendarSyncError(null);
        meeting.setCalendarSyncedAt(Instant.now());
        meetingRepository.save(meeting);
    }

    @Override
    @Transactional
    public void markFailed(Long meetingId, String errorMessage) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Rapat tidak ditemukan: " + meetingId));
        meeting.setCalendarSyncStatus(CalendarSyncStatus.FAILED);
        meeting.setCalendarSyncError(errorMessage);
        meetingRepository.save(meeting);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canRetrySync(User user, Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Rapat tidak ditemukan: " + meetingId));
        return user.getRole() == Role.ADMIN || user.getId().equals(meeting.getOrganizer().getId());
    }

    @Override
    public void assertCanRetrySync(User user, Long meetingId) {
        if (!canRetrySync(user, meetingId)) {
            throw new AccessDeniedException("Anda tidak berhak menjalankan ulang sync rapat ini");
        }
    }
}
