package id.jagr.rapat.security;

import java.util.Set;

import org.junit.jupiter.api.Test;

import id.jagr.rapat.user.AppRole;
import id.jagr.rapat.user.AppRoleFixtures;
import id.jagr.rapat.user.Capability;
import id.jagr.rapat.user.User;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    private Capability capability(String code) {
        Capability capability = new Capability();
        capability.setCode(code);
        capability.setLabel(code);
        return capability;
    }

    @Test
    void builtInAdminRoleGrantsRoleAuthorityPlusCanOrganizeMeetings() {
        User user = new User();
        user.setEmail("test@company.local");
        user.setRole(AppRoleFixtures.admin());
        UserPrincipal principal = new UserPrincipal(user);

        // Admin's AppRole is seeded canOrganizeMeetings=true for isOrganizerCapable's sake
        // (issue #45) -- MeetingController's own division guard is what actually keeps Admin
        // from creating meetings despite carrying this authority (issue #53).
        assertThat(principal.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "CAN_ORGANIZE_MEETINGS");
    }

    @Test
    void builtInKaryawanRoleGrantsOnlyItsRoleAuthority() {
        User user = new User();
        user.setEmail("test@company.local");
        user.setRole(AppRoleFixtures.karyawan());
        UserPrincipal principal = new UserPrincipal(user);

        assertThat(principal.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_KARYAWAN");
    }

    @Test
    void customRoleGrantsRoleAuthorityPlusCapabilitiesPlusCanOrganizeMeetings() {
        AppRole custom = new AppRole("Manajer Regional", false, true, false, false, true);
        custom.setCapabilities(Set.of(capability("MANAGE_DIVISIONS"), capability("MANAGE_TELEGRAM_GROUPS")));
        User user = new User();
        user.setEmail("test@company.local");
        user.setRole(custom);
        UserPrincipal principal = new UserPrincipal(user);

        assertThat(principal.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder(
                        "ROLE_Manajer Regional",
                        "CAPABILITY_MANAGE_DIVISIONS",
                        "CAPABILITY_MANAGE_TELEGRAM_GROUPS",
                        "CAN_ORGANIZE_MEETINGS");
    }

    @Test
    void customRoleWithoutCanOrganizeMeetingsFlagDoesNotGrantThatAuthority() {
        AppRole custom = new AppRole("Peninjau", false, false, false, true, false);
        User user = new User();
        user.setEmail("test@company.local");
        user.setRole(custom);
        UserPrincipal principal = new UserPrincipal(user);

        assertThat(principal.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_Peninjau");
    }
}
