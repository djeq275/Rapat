package id.jagr.rapat.meeting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import id.jagr.rapat.division.Division;
import id.jagr.rapat.user.AppRole;
import id.jagr.rapat.user.AppRoleFixtures;
import id.jagr.rapat.user.User;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class MeetingAccessServiceTest {

    @Mock
    MeetingRepository meetingRepository;

    MeetingAccessService service;

    private Meeting meetingInDivision(Division division) {
        Meeting meeting = new Meeting();
        meeting.setDivision(division);
        return meeting;
    }

    private User userWith(AppRole role, Division division) {
        User user = new User();
        user.setRole(role);
        user.setDivision(division);
        return user;
    }

    @Test
    void karyawanCanViewOwnDivisionMeeting() {
        service = new MeetingAccessService(meetingRepository);
        Division division = new Division("Engineering");
        division.setId(1L);
        User viewer = userWith(AppRoleFixtures.karyawan(), division);

        assertThatCode(() -> service.assertCanView(viewer, meetingInDivision(division))).doesNotThrowAnyException();
    }

    @Test
    void karyawanCannotViewOtherDivisionMeeting() {
        service = new MeetingAccessService(meetingRepository);
        Division own = new Division("Engineering");
        own.setId(1L);
        Division other = new Division("Sales");
        other.setId(2L);
        User viewer = userWith(AppRoleFixtures.karyawan(), own);

        assertThatThrownBy(() -> service.assertCanView(viewer, meetingInDivision(other)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void direkturCanViewAnyDivisionMeeting() {
        service = new MeetingAccessService(meetingRepository);
        Division other = new Division("Sales");
        other.setId(2L);
        User viewer = userWith(AppRoleFixtures.direktur(), null);

        assertThatCode(() -> service.assertCanView(viewer, meetingInDivision(other))).doesNotThrowAnyException();
    }

    @Test
    void adminCanViewAnyDivisionMeeting() {
        service = new MeetingAccessService(meetingRepository);
        Division other = new Division("Sales");
        other.setId(2L);
        User viewer = userWith(AppRoleFixtures.admin(), null);

        assertThatCode(() -> service.assertCanView(viewer, meetingInDivision(other))).doesNotThrowAnyException();
    }

    @Test
    void customRoleWithCanViewAllDivisionsFlagCanViewAnyDivisionMeeting() {
        service = new MeetingAccessService(meetingRepository);
        Division other = new Division("Sales");
        other.setId(2L);
        AppRole direksiRegional = new AppRole("Direksi Regional", false, false, false, true, false);
        User viewer = userWith(direksiRegional, null);

        assertThatCode(() -> service.assertCanView(viewer, meetingInDivision(other))).doesNotThrowAnyException();
    }

    @Test
    void customRoleWithoutCanViewAllDivisionsFlagCannotViewOtherDivisionMeeting() {
        service = new MeetingAccessService(meetingRepository);
        Division own = new Division("Engineering");
        own.setId(1L);
        Division other = new Division("Sales");
        other.setId(2L);
        AppRole peninjauDivisi = new AppRole("Peninjau Divisi", false, true, false, false, false);
        User viewer = userWith(peninjauDivisi, own);

        assertThatThrownBy(() -> service.assertCanView(viewer, meetingInDivision(other)))
                .isInstanceOf(AccessDeniedException.class);
    }
}
