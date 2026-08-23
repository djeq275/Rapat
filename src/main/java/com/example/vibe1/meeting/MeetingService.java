package com.example.vibe1.meeting;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.vibe1.division.Division;
import com.example.vibe1.division.DivisionRepository;
import com.example.vibe1.user.Role;
import com.example.vibe1.user.User;
import com.example.vibe1.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetingService implements MeetingSyncQueryPort, MeetingSyncStatusPort, MeetingSyncAuthorizationPort {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final DivisionRepository divisionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

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

        eventPublisher.publishEvent(new MeetingScheduledEvent(meeting.getId()));
        return meeting;
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
