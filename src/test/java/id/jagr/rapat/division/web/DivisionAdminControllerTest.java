package id.jagr.rapat.division.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import id.jagr.rapat.division.DivisionService;
import id.jagr.rapat.security.AppOidcUserService;
import id.jagr.rapat.security.SecurityConfig;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DivisionAdminController.class)
@Import(SecurityConfig.class)
class DivisionAdminControllerTest {

    @MockitoBean
    DivisionService divisionService;
    @MockitoBean
    AppOidcUserService appOidcUserService;
    @MockitoBean
    ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    MockMvc mockMvc;

    @Test
    void nonAdminGets403() throws Exception {
        mockMvc.perform(get("/admin/divisions").with(user("karyawan@company.local").roles("KARYAWAN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessList() throws Exception {
        when(divisionService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/divisions").with(user("admin@company.local").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void customRoleWithMatchingCapabilityCanAccessList() throws Exception {
        when(divisionService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/divisions").with(user("manajer@company.local")
                        .authorities(new SimpleGrantedAuthority("CAPABILITY_MANAGE_DIVISIONS"))))
                .andExpect(status().isOk());
    }

    @Test
    void customRoleWithoutMatchingCapabilityStays403() throws Exception {
        mockMvc.perform(get("/admin/divisions").with(user("manajer@company.local")
                        .authorities(new SimpleGrantedAuthority("CAPABILITY_MANAGE_USERS"))))
                .andExpect(status().isForbidden());
    }
}
