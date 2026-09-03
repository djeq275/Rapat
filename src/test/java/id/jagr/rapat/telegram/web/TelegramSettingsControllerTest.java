package id.jagr.rapat.telegram.web;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import id.jagr.rapat.security.AppOidcUserService;
import id.jagr.rapat.security.SecurityConfig;
import id.jagr.rapat.telegram.TelegramBotConfigService;

import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The saved token must never come back out through this page's HTML. */
@WebMvcTest(TelegramSettingsController.class)
@Import(SecurityConfig.class)
class TelegramSettingsControllerTest {

    @MockitoBean
    TelegramBotConfigService telegramBotConfigService;
    @MockitoBean
    AppOidcUserService appOidcUserService;
    @MockitoBean
    ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    MockMvc mockMvc;

    @Test
    void nonAdminGets403() throws Exception {
        mockMvc.perform(get("/admin/telegram/settings").with(user("karyawan@company.local").roles("KARYAWAN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenValueNeverAppearsInResponseBody() throws Exception {
        when(telegramBotConfigService.currentToken()).thenReturn(Optional.of("123456:super-secret-value"));

        mockMvc.perform(get("/admin/telegram/settings").with(user("admin@company.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("super-secret-value"))));
    }
}
