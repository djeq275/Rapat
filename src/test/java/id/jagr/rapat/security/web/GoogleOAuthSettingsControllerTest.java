package id.jagr.rapat.security.web;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import id.jagr.rapat.security.AppOidcUserService;
import id.jagr.rapat.security.GoogleOAuthConfig;
import id.jagr.rapat.security.GoogleOAuthConfigService;
import id.jagr.rapat.security.SecurityConfig;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The saved client secret must never come back out through this page's HTML. */
@WebMvcTest(GoogleOAuthSettingsController.class)
@Import(SecurityConfig.class)
class GoogleOAuthSettingsControllerTest {

    @MockitoBean
    GoogleOAuthConfigService googleOAuthConfigService;
    @MockitoBean
    AppOidcUserService appOidcUserService;
    @MockitoBean
    ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    MockMvc mockMvc;

    @Test
    void nonAdminGets403() throws Exception {
        mockMvc.perform(get("/admin/google-oauth/settings").with(user("karyawan@company.local").roles("KARYAWAN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void secretValueNeverAppearsInResponseBody() throws Exception {
        GoogleOAuthConfig config = new GoogleOAuthConfig();
        config.setClientId("rapat-app-client-id");
        config.setClientSecretEnc("super-secret-value");
        when(googleOAuthConfigService.currentConfig()).thenReturn(Optional.of(config));
        when(googleOAuthConfigService.isConfigured()).thenReturn(true);

        mockMvc.perform(get("/admin/google-oauth/settings").with(user("admin@company.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("super-secret-value"))))
                .andExpect(content().string(containsString("rapat-app-client-id")));
    }

    @Test
    void savingWithBlankSecretOnFirstSetupShowsErrorInsteadOf500() throws Exception {
        doThrow(new IllegalArgumentException("Client secret wajib diisi saat pertama kali mengatur konfigurasi Google OAuth"))
                .when(googleOAuthConfigService).save(any(), any());

        mockMvc.perform(post("/admin/google-oauth/settings")
                        .with(user("admin@company.local").roles("ADMIN"))
                        .with(csrf())
                        .param("clientId", "rapat-app-client-id")
                        .param("clientSecret", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("wajib diisi")));
    }
}
