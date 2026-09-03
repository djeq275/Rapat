package id.jagr.rapat.meeting;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import id.jagr.rapat.division.Division;
import id.jagr.rapat.division.DivisionRepository;
import id.jagr.rapat.telegram.MeetingTelegramNotificationService;
import id.jagr.rapat.user.Role;
import id.jagr.rapat.user.User;
import id.jagr.rapat.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    MeetingRepository meetingRepository;
    @Mock
    MeetingParticipantRepository participantRepository;
    @Mock
    DivisionRepository divisionRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @Mock
    MeetingTelegramNotificationService meetingTelegramNotificationService;

    MeetingService service;

    @Test
    void direkturIsAlwaysAddedExactlyOnceEvenIfAlsoSelected() {
        service = new MeetingService(meetingRepository, participantRepository, divisionRepository, userRepository, eventPublisher, meetingTelegramNotificationService);

        Division division = new Division("Engineering");
        division.setId(1L);

        User organizer = new User();
        organizer.setId(10L);
        organizer.setRole(Role.KETUA_DIVISI);
        organizer.setDivision(division);

        User karyawan = new User();
        karyawan.setId(20L);
        karyawan.setRole(Role.KARYAWAN);
        karyawan.setDivision(division);

        User direktur = new User();
        direktur.setId(99L);
        direktur.setRole(Role.DIREKTUR);

        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(userRepository.findById(10L)).thenReturn(Optional.of(organizer));
        when(userRepository.findById(20L)).thenReturn(Optional.of(karyawan));
        when(userRepository.findByRole(Role.DIREKTUR)).thenReturn(List.of(direktur));
        lenient().when(meetingRepository.save(any())).thenAnswer(invocation -> {
            Meeting m = invocation.getArgument(0);
            if (m.getId() == null) {
                m.setId(100L);
            }
            return m;
        });
        when(participantRepository.existsByMeetingIdAndUserId(anyLong(), any())).thenReturn(false);

        Instant start = Instant.now();
        CreateMeetingCommand command = new CreateMeetingCommand(
                "Rapat Mingguan", null, null, start, start.plus(1, ChronoUnit.HOURS),
                1L, 10L, List.of(20L), List.of());

        service.create(command);

        ArgumentCaptor<MeetingParticipant> captor = ArgumentCaptor.forClass(MeetingParticipant.class);
        verify(participantRepository, times(2)).save(captor.capture());

        List<MeetingParticipant> saved = captor.getAllValues();
        assertThat(saved).extracting(p -> p.getUser().getId()).containsExactlyInAnyOrder(20L, 99L);
        assertThat(saved.stream().filter(p -> p.getUser().getId().equals(99L)).findFirst().orElseThrow().getAddedReason())
                .isEqualTo(ParticipantAddedReason.DIREKTUR_AUTO);

        verify(eventPublisher).publishEvent(new MeetingScheduledEvent(100L));
    }

    @Test
    void rejectsOrganizerFromDifferentDivision() {
        service = new MeetingService(meetingRepository, participantRepository, divisionRepository, userRepository, eventPublisher, meetingTelegramNotificationService);

        Division division = new Division("Engineering");
        division.setId(1L);
        Division otherDivision = new Division("Sales");
        otherDivision.setId(2L);

        User organizer = new User();
        organizer.setId(10L);
        organizer.setRole(Role.KETUA_DIVISI);
        organizer.setDivision(otherDivision);

        when(divisionRepository.findById(1L)).thenReturn(Optional.of(division));
        when(userRepository.findById(10L)).thenReturn(Optional.of(organizer));

        Instant start = Instant.now();
        CreateMeetingCommand command = new CreateMeetingCommand(
                "Rapat", null, null, start, start.plus(1, ChronoUnit.HOURS), 1L, 10L, List.of(), List.of());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> service.create(command));
        verify(eventPublisher, never()).publishEvent(any());
    }
}
