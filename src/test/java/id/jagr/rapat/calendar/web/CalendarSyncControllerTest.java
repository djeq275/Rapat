package id.jagr.rapat.calendar.web;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import id.jagr.rapat.calendar.CalendarSyncService;
import id.jagr.rapat.meeting.MeetingSyncAuthorizationPort;
import id.jagr.rapat.security.AppOidcUserService;
import id.jagr.rapat.security.SecurityConfig;
import id.jagr.rapat.user.Role;
import id.jagr.rapat.user.User;
import id.jagr.rapat.user.UserRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gate for POST /meetings/{id}/retry-sync -- only the meeting's organizer or
 * Admin may trigger it (see MeetingService#assertCanRetrySync); anyone else
 * must get a 403 without the sync actually running.
 */
@WebMvcTest(CalendarSyncController.class)
@Import(SecurityConfig.class)
class CalendarSyncControllerTest {

    @MockitoBean
    CalendarSyncService calendarSyncService;
    @MockitoBean
    MeetingSyncAuthorizationPort meetingSyncAuthorizationPort;
    @MockitoBean
    UserRepository userRepository;
    @MockitoBean
    AppOidcUserService appOidcUserService;
    @MockitoBean
    ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    MockMvc mockMvc;

    @Test
    void deniedRequesterGets403AndNeverSyncs() throws Exception {
        User otherKetua = new User();
        otherKetua.setId(7L);
        otherKetua.setEmail("ketua-lain@company.local");
        otherKetua.setRole(Role.KETUA_DIVISI);

        when(userRepository.findByEmailIgnoreCase("ketua-lain@company.local")).thenReturn(Optional.of(otherKetua));
        doThrow(new AccessDeniedException("Anda tidak berhak menjalankan ulang sync rapat ini"))
                .when(meetingSyncAuthorizationPort).assertCanRetrySync(eq(otherKetua), eq(9L));

        mockMvc.perform(post("/meetings/9/retry-sync")
                        .with(user("ketua-lain@company.local").roles("KETUA_DIVISI"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(calendarSyncService, never()).sync(any());
    }

    @Test
    void organizerCanRetry() throws Exception {
        User organizer = new User();
        organizer.setId(1L);
        organizer.setEmail("ketua@company.local");
        organizer.setRole(Role.KETUA_DIVISI);

        when(userRepository.findByEmailIgnoreCase("ketua@company.local")).thenReturn(Optional.of(organizer));

        mockMvc.perform(post("/meetings/9/retry-sync")
                        .with(user("ketua@company.local").roles("KETUA_DIVISI"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(meetingSyncAuthorizationPort).assertCanRetrySync(organizer, 9L);
        verify(calendarSyncService).sync(9L);
    }
}
