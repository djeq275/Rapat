package id.jagr.rapat.user;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import id.jagr.rapat.user.web.RoleForm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    AppRoleRepository appRoleRepository;
    @Mock
    CapabilityRepository capabilityRepository;
    @Mock
    UserRepository userRepository;

    RoleService service;

    private RoleForm form(String name) {
        RoleForm form = new RoleForm();
        form.setName(name);
        return form;
    }

    @Test
    void createRejectsDuplicateName() {
        service = new RoleService(appRoleRepository, capabilityRepository, userRepository);
        AppRole existing = AppRoleFixtures.karyawan();
        existing.setId(4L);
        when(appRoleRepository.findByNameIgnoreCase("Manajer")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(form("Manajer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sudah dipakai");
    }

    @Test
    void createSavesFlagsAndCapabilities() {
        service = new RoleService(appRoleRepository, capabilityRepository, userRepository);
        when(appRoleRepository.findByNameIgnoreCase("Manajer Regional")).thenReturn(Optional.empty());
        Capability manageUsers = new Capability();
        manageUsers.setId(1L);
        manageUsers.setCode("MANAGE_USERS");
        manageUsers.setLabel("Kelola Pengguna");
        when(capabilityRepository.findAllById(List.of(1L))).thenReturn(List.of(manageUsers));
        when(appRoleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RoleForm form = form("Manajer Regional");
        form.setRequiresDivision(true);
        form.setCanOrganizeMeetings(true);
        form.setCapabilityIds(List.of(1L));

        AppRole created = service.create(form);

        assertThat(created.isBuiltIn()).isFalse();
        assertThat(created.isRequiresDivision()).isTrue();
        assertThat(created.isCanOrganizeMeetings()).isTrue();
        assertThat(created.isAutoInviteToAllMeetings()).isFalse();
        assertThat(created.getCapabilities()).containsExactly(manageUsers);
    }

    @Test
    void updateRejectsBuiltInRole() {
        service = new RoleService(appRoleRepository, capabilityRepository, userRepository);
        AppRole admin = AppRoleFixtures.admin();
        admin.setId(1L);
        when(appRoleRepository.findByIdWithCapabilities(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.update(1L, form("Admin Baru")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bawaan");

        verify(appRoleRepository, never()).save(any());
    }

    @Test
    void deleteRejectsBuiltInRole() {
        service = new RoleService(appRoleRepository, capabilityRepository, userRepository);
        AppRole direktur = AppRoleFixtures.direktur();
        direktur.setId(2L);
        when(appRoleRepository.findByIdWithCapabilities(2L)).thenReturn(Optional.of(direktur));

        assertThatThrownBy(() -> service.delete(2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bawaan");

        verify(appRoleRepository, never()).delete(any());
    }

    @Test
    void deleteRejectsCustomRoleStillInUse() {
        service = new RoleService(appRoleRepository, capabilityRepository, userRepository);
        AppRole custom = new AppRole("Manajer Regional", false, true, false, false, true);
        custom.setId(5L);
        when(appRoleRepository.findByIdWithCapabilities(5L)).thenReturn(Optional.of(custom));
        when(userRepository.existsByRole(custom)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dipakai");

        verify(appRoleRepository, never()).delete(any());
    }

    @Test
    void deleteSucceedsForUnusedCustomRole() {
        service = new RoleService(appRoleRepository, capabilityRepository, userRepository);
        AppRole custom = new AppRole("Manajer Regional", false, true, false, false, true);
        custom.setId(5L);
        when(appRoleRepository.findByIdWithCapabilities(5L)).thenReturn(Optional.of(custom));
        lenient().when(userRepository.existsByRole(custom)).thenReturn(false);

        service.delete(5L);

        ArgumentCaptor<AppRole> captor = ArgumentCaptor.forClass(AppRole.class);
        verify(appRoleRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(5L);
    }
}
