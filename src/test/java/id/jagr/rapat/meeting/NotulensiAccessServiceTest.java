package id.jagr.rapat.meeting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import id.jagr.rapat.division.Division;
import id.jagr.rapat.user.Role;
import id.jagr.rapat.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Covers the permission matrix the PRD flags as the top implementation risk for notulensi. */
@ExtendWith(MockitoExtension.class)
class NotulensiAccessServiceTest {

    @Mock
    MeetingNotetakerRepository notetakerRepository;

    NotulensiAccessService service;

    private Division division(Long id) {
        Division division = new Division("Engineering");
        division.setId(id);
        return division;
    }

    private User user(Role role, Long id, Division division) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setDivision(division);
        return user;
    }

    @Test
    void adminCanAlwaysWriteAndManage() {
        service = new NotulensiAccessService(notetakerRepository);
        Meeting meeting = new Meeting();
        meeting.setId(1L);
        meeting.setDivision(division(1L));
        User admin = user(Role.ADMIN, 1L, null);

        assertThat(service.canWrite(admin, meeting)).isTrue();
        assertThat(service.canManageNotetakers(admin, meeting)).isTrue();
    }

    @Test
    void ketuaDivisiOfSameDivisionCanWriteWithoutBeingGrantedAnything() {
        service = new NotulensiAccessService(notetakerRepository);
        Division division = division(1L);
        Meeting meeting = new Meeting();
        meeting.setId(1L);
        meeting.setDivision(division);
        User ketua = user(Role.KETUA_DIVISI, 2L, division);

        assertThat(service.canWrite(ketua, meeting)).isTrue();
        assertThat(service.canManageNotetakers(ketua, meeting)).isTrue();
    }

    @Test
    void assignedNotetakerCanWriteButNotManage() {
        service = new NotulensiAccessService(notetakerRepository);
        Meeting meeting = new Meeting();
        meeting.setId(1L);
        meeting.setDivision(division(1L));
        User karyawan = user(Role.KARYAWAN, 3L, division(1L));

        when(notetakerRepository.existsByMeetingIdAndUserId(1L, 3L)).thenReturn(true);

        assertThat(service.canWrite(karyawan, meeting)).isTrue();
        assertThat(service.canManageNotetakers(karyawan, meeting)).isFalse();
    }

    @Test
    void ordinaryParticipantCannotWriteOrManage() {
        service = new NotulensiAccessService(notetakerRepository);
        Meeting meeting = new Meeting();
        meeting.setId(1L);
        meeting.setDivision(division(1L));
        User karyawan = user(Role.KARYAWAN, 4L, division(1L));

        lenient().when(notetakerRepository.existsByMeetingIdAndUserId(1L, 4L)).thenReturn(false);

        assertThat(service.canWrite(karyawan, meeting)).isFalse();
        assertThat(service.canManageNotetakers(karyawan, meeting)).isFalse();
    }

    @Test
    void ketuaDivisiOfDifferentDivisionCannotWriteOrManage() {
        service = new NotulensiAccessService(notetakerRepository);
        Meeting meeting = new Meeting();
        meeting.setId(1L);
        meeting.setDivision(division(1L));
        User otherKetua = user(Role.KETUA_DIVISI, 5L, division(2L));

        lenient().when(notetakerRepository.existsByMeetingIdAndUserId(1L, 5L)).thenReturn(false);

        assertThat(service.canWrite(otherKetua, meeting)).isFalse();
        assertThat(service.canManageNotetakers(otherKetua, meeting)).isFalse();
    }
}
