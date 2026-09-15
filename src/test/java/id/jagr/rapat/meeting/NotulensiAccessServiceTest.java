package id.jagr.rapat.meeting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import id.jagr.rapat.division.Division;
import id.jagr.rapat.user.AppRole;
import id.jagr.rapat.user.AppRoleFixtures;
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

    private User user(AppRole role, Long id, Division division) {
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
        User admin = user(AppRoleFixtures.admin(), 1L, null);

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
        User ketua = user(AppRoleFixtures.ketuaDivisi(), 2L, division);

        assertThat(service.canWrite(ketua, meeting)).isTrue();
        assertThat(service.canManageNotetakers(ketua, meeting)).isTrue();
    }

    @Test
    void assignedNotetakerCanWriteButNotManage() {
        service = new NotulensiAccessService(notetakerRepository);
        Meeting meeting = new Meeting();
        meeting.setId(1L);
        meeting.setDivision(division(1L));
        User karyawan = user(AppRoleFixtures.karyawan(), 3L, division(1L));

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
        User karyawan = user(AppRoleFixtures.karyawan(), 4L, division(1L));

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
        User otherKetua = user(AppRoleFixtures.ketuaDivisi(), 5L, division(2L));

        lenient().when(notetakerRepository.existsByMeetingIdAndUserId(1L, 5L)).thenReturn(false);

        assertThat(service.canWrite(otherKetua, meeting)).isFalse();
        assertThat(service.canManageNotetakers(otherKetua, meeting)).isFalse();
    }

    @Test
    void customRoleWithOrganizeAndViewAllFlagsCanAlwaysWriteAndManage() {
        service = new NotulensiAccessService(notetakerRepository);
        Meeting meeting = new Meeting();
        meeting.setId(1L);
        meeting.setDivision(division(1L));
        AppRole direksiOperasional = new AppRole("Direksi Operasional", false, false, false, true, true);
        User user = user(direksiOperasional, 6L, null);

        assertThat(service.canWrite(user, meeting)).isTrue();
        assertThat(service.canManageNotetakers(user, meeting)).isTrue();
    }

    @Test
    void customRoleWithOrganizeFlagOnlyManagesOwnDivision() {
        service = new NotulensiAccessService(notetakerRepository);
        Division division = division(1L);
        Meeting meeting = new Meeting();
        meeting.setId(1L);
        meeting.setDivision(division);
        AppRole manajerRegional = new AppRole("Manajer Regional", false, true, false, false, true);
        User sameDivision = user(manajerRegional, 7L, division);
        User otherDivision = user(manajerRegional, 8L, division(2L));

        lenient().when(notetakerRepository.existsByMeetingIdAndUserId(1L, 8L)).thenReturn(false);

        assertThat(service.canWrite(sameDivision, meeting)).isTrue();
        assertThat(service.canManageNotetakers(sameDivision, meeting)).isTrue();
        assertThat(service.canWrite(otherDivision, meeting)).isFalse();
        assertThat(service.canManageNotetakers(otherDivision, meeting)).isFalse();
    }

    /** Direktur has canViewAllDivisions=true but not canOrganizeMeetings -- viewing all meetings must not imply managing their notulensi. */
    @Test
    void directurCannotManageNotetakersDespiteViewingAllDivisions() {
        service = new NotulensiAccessService(notetakerRepository);
        Meeting meeting = new Meeting();
        meeting.setId(1L);
        meeting.setDivision(division(1L));
        User direktur = user(AppRoleFixtures.direktur(), 9L, null);

        lenient().when(notetakerRepository.existsByMeetingIdAndUserId(1L, 9L)).thenReturn(false);

        assertThat(service.canManageNotetakers(direktur, meeting)).isFalse();
    }
}
