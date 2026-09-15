package id.jagr.rapat.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** isOrganizerCapable() gates the Google Calendar consent scope -- must match canOrganizeMeetings exactly (issue #47). */
class UserTest {

    private User userWith(AppRole role) {
        User user = new User();
        user.setRole(role);
        return user;
    }

    @Test
    void adminIsOrganizerCapable() {
        assertThat(userWith(AppRoleFixtures.admin()).isOrganizerCapable()).isTrue();
    }

    @Test
    void ketuaDivisiIsOrganizerCapable() {
        assertThat(userWith(AppRoleFixtures.ketuaDivisi()).isOrganizerCapable()).isTrue();
    }

    @Test
    void direkturIsNotOrganizerCapable() {
        assertThat(userWith(AppRoleFixtures.direktur()).isOrganizerCapable()).isFalse();
    }

    @Test
    void karyawanIsNotOrganizerCapable() {
        assertThat(userWith(AppRoleFixtures.karyawan()).isOrganizerCapable()).isFalse();
    }

    @Test
    void customRoleWithCanOrganizeMeetingsFlagIsOrganizerCapable() {
        AppRole custom = new AppRole("Manajer Regional", false, true, false, false, true);
        assertThat(userWith(custom).isOrganizerCapable()).isTrue();
    }
}
