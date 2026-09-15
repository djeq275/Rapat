package id.jagr.rapat.user.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import id.jagr.rapat.security.AppOidcUserService;
import id.jagr.rapat.security.SecurityConfig;
import id.jagr.rapat.user.AppRole;
import id.jagr.rapat.user.AppRoleFixtures;
import id.jagr.rapat.user.RoleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleAdminController.class)
@Import(SecurityConfig.class)
class RoleAdminControllerTest {

    @MockitoBean
    RoleService roleService;
    @MockitoBean
    AppOidcUserService appOidcUserService;
    @MockitoBean
    ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    MockMvc mockMvc;

    @Test
    void nonAdminGets403() throws Exception {
        mockMvc.perform(get("/admin/roles").with(user("karyawan@company.local").roles("KARYAWAN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessList() throws Exception {
        when(roleService.findAll()).thenReturn(List.of(AppRoleFixtures.admin()));

        mockMvc.perform(get("/admin/roles").with(user("admin@company.local").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void builtInRoleFormHasNoSubmitButton() throws Exception {
        AppRole admin = AppRoleFixtures.admin();
        admin.setId(1L);
        when(roleService.findById(1L)).thenReturn(admin);

        mockMvc.perform(get("/admin/roles/1/edit").with(user("admin@company.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(">Simpan<"))));
    }

    @Test
    void updateShowsErrorWhenServiceRejectsBuiltInRole() throws Exception {
        AppRole admin = AppRoleFixtures.admin();
        admin.setId(1L);
        when(roleService.findById(1L)).thenReturn(admin);
        doThrow(new IllegalArgumentException("Role bawaan tidak bisa diubah"))
                .when(roleService).update(eq(1L), any());

        mockMvc.perform(post("/admin/roles/1")
                        .with(user("admin@company.local").roles("ADMIN"))
                        .with(csrf())
                        .param("name", "Admin Baru"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Role bawaan tidak bisa diubah")));
    }

    @Test
    void createBindsFormFieldsAndRedirectsOnSuccess() throws Exception {
        AppRole created = new AppRole("Manajer Regional", false, true, false, false, true);
        when(roleService.create(any())).thenReturn(created);

        mockMvc.perform(post("/admin/roles")
                        .with(user("admin@company.local").roles("ADMIN"))
                        .with(csrf())
                        .param("name", "Manajer Regional")
                        .param("requiresDivision", "true")
                        .param("canOrganizeMeetings", "true")
                        .param("capabilityIds", "1", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles"));

        ArgumentCaptor<RoleForm> captor = ArgumentCaptor.forClass(RoleForm.class);
        verify(roleService).create(captor.capture());
        RoleForm bound = captor.getValue();
        assertThat(bound.getName()).isEqualTo("Manajer Regional");
        assertThat(bound.isRequiresDivision()).isTrue();
        assertThat(bound.isCanOrganizeMeetings()).isTrue();
        assertThat(bound.isAutoInviteToAllMeetings()).isFalse();
        assertThat(bound.getCapabilityIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void deleteRedirectsWithFlashErrorWhenRoleStillInUse() throws Exception {
        doThrow(new IllegalArgumentException("Role masih dipakai oleh pengguna, tidak bisa dihapus"))
                .when(roleService).delete(5L);

        mockMvc.perform(post("/admin/roles/5/delete")
                        .with(user("admin@company.local").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/roles"));

        verify(roleService).delete(5L);
    }
}
