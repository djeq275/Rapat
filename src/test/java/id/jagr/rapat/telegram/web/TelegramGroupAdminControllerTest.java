package id.jagr.rapat.telegram.web;

import java.util.List;
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
import id.jagr.rapat.telegram.TelegramChatCandidate;
import id.jagr.rapat.telegram.TelegramGateway;
import id.jagr.rapat.telegram.TelegramGroup;
import id.jagr.rapat.telegram.TelegramGroupService;
import id.jagr.rapat.telegram.TelegramSendException;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelegramGroupAdminController.class)
@Import(SecurityConfig.class)
class TelegramGroupAdminControllerTest {

    @MockitoBean
    TelegramGroupService telegramGroupService;
    @MockitoBean
    TelegramBotConfigService telegramBotConfigService;
    @MockitoBean
    TelegramGateway telegramGateway;
    @MockitoBean
    AppOidcUserService appOidcUserService;
    @MockitoBean
    ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    MockMvc mockMvc;

    @Test
    void nonAdminGets403() throws Exception {
        mockMvc.perform(get("/admin/telegram-groups").with(user("karyawan@company.local").roles("KARYAWAN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessList() throws Exception {
        when(telegramGroupService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/telegram-groups").with(user("admin@company.local").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void tokenNotConfiguredHidesChatCandidates() throws Exception {
        when(telegramBotConfigService.currentToken()).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/telegram-groups/new").with(user("admin@company.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tokenConfigured", false))
                .andExpect(model().attributeDoesNotExist("chatCandidates"));

        verifyNoInteractions(telegramGateway);
    }

    @Test
    void gatewayFailureDegradesToEmptyCandidateListInsteadOfFailingThePage() throws Exception {
        when(telegramBotConfigService.currentToken()).thenReturn(Optional.of("token"));
        when(telegramGroupService.findAll()).thenReturn(List.of());
        when(telegramGateway.fetchRecentChats()).thenThrow(new TelegramSendException("rate limited"));

        mockMvc.perform(get("/admin/telegram-groups/new").with(user("admin@company.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tokenConfigured", true))
                .andExpect(model().attribute("chatCandidates", List.of()));
    }

    @Test
    void alreadyRegisteredChatIdsAreFilteredOutOfCandidates() throws Exception {
        when(telegramBotConfigService.currentToken()).thenReturn(Optional.of("token"));
        when(telegramGroupService.findAll()).thenReturn(List.of(new TelegramGroup("Grup Lama", "-100111")));
        when(telegramGateway.fetchRecentChats()).thenReturn(List.of(
                new TelegramChatCandidate("-100111", "Grup Lama"),
                new TelegramChatCandidate("-100222", "Grup Baru")));

        mockMvc.perform(get("/admin/telegram-groups/new").with(user("admin@company.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("chatCandidates", List.of(new TelegramChatCandidate("-100222", "Grup Baru"))));
    }
}
