package com.example.vibe1.meeting.web;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.vibe1.division.Division;
import com.example.vibe1.meeting.Meeting;
import com.example.vibe1.meeting.MeetingAccessService;
import com.example.vibe1.meeting.MeetingParticipantRepository;
import com.example.vibe1.meeting.MeetingRepository;
import com.example.vibe1.meeting.MeetingService;
import com.example.vibe1.meeting.NotetakerService;
import com.example.vibe1.meeting.NotulensiAccessService;
import com.example.vibe1.security.AppOidcUserService;
import com.example.vibe1.security.SecurityConfig;
import com.example.vibe1.telegram.DivisionTelegramGroupService;
import com.example.vibe1.telegram.MeetingTelegramNotificationService;
import com.example.vibe1.telegram.TelegramGroupService;
import com.example.vibe1.user.Role;
import com.example.vibe1.user.User;
import com.example.vibe1.user.UserRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the controller actually enforces {@link MeetingAccessService#assertCanView}
 * on a direct /meetings/{id} hit -- list-query scoping alone would not catch a
 * Karyawan reaching another division's meeting by guessing its id.
 */
@WebMvcTest(MeetingController.class)
@Import(SecurityConfig.class)
class MeetingControllerAccessTest {

    @MockitoBean
    MeetingService meetingService;
    @MockitoBean
    MeetingAccessService meetingAccessService;
    @MockitoBean
    NotulensiAccessService notulensiAccessService;
    @MockitoBean
    NotetakerService notetakerService;
    @MockitoBean
    MeetingRepository meetingRepository;
    @MockitoBean
    MeetingParticipantRepository participantRepository;
    @MockitoBean
    UserRepository userRepository;
    @MockitoBean
    AppOidcUserService appOidcUserService;
    @MockitoBean
    TelegramGroupService telegramGroupService;
    @MockitoBean
    DivisionTelegramGroupService divisionTelegramGroupService;
    @MockitoBean
    MeetingTelegramNotificationService meetingTelegramNotificationService;

    @Autowired
    MockMvc mockMvc;

    @Test
    void deniedAccessSurfacesAs403() throws Exception {
        Division otherDivision = new Division("Sales");
        otherDivision.setId(2L);
        Meeting meeting = new Meeting();
        meeting.setDivision(otherDivision);

        User karyawan = new User();
        karyawan.setEmail("karyawan@company.local");
        karyawan.setRole(Role.KARYAWAN);

        when(userRepository.findByEmailIgnoreCase("karyawan@company.local")).thenReturn(Optional.of(karyawan));
        when(meetingRepository.findDetailById(5L)).thenReturn(Optional.of(meeting));
        doThrow(new AccessDeniedException("Anda tidak berhak melihat rapat ini"))
                .when(meetingAccessService).assertCanView(eq(karyawan), any());

        mockMvc.perform(get("/meetings/5").with(user("karyawan@company.local").roles("KARYAWAN")))
                .andExpect(status().isForbidden());
    }

    /**
     * The Telegram retry action reuses {@link com.example.vibe1.meeting.MeetingSyncAuthorizationPort},
     * the same organizer-or-Admin rule as Calendar sync retry -- see {@code CalendarSyncControllerTest}
     * for the analogous coverage on that endpoint.
     */
    @Test
    void telegramRetryDeniedForNonOrganizerSurfacesAs403AndNeverRetries() throws Exception {
        User otherKetua = new User();
        otherKetua.setEmail("ketua-lain@company.local");
        otherKetua.setRole(Role.KETUA_DIVISI);

        when(userRepository.findByEmailIgnoreCase("ketua-lain@company.local")).thenReturn(Optional.of(otherKetua));
        doThrow(new AccessDeniedException("Anda tidak berhak menjalankan ulang sync rapat ini"))
                .when(meetingService).assertCanRetrySync(eq(otherKetua), eq(9L));

        mockMvc.perform(post("/meetings/9/telegram-groups/3/retry")
                        .with(user("ketua-lain@company.local").roles("KETUA_DIVISI"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(meetingTelegramNotificationService, never()).retry(any(), any());
    }

    @Test
    void telegramRetryAllowedForOrganizerRedirectsAndRetries() throws Exception {
        User organizer = new User();
        organizer.setEmail("ketua@company.local");
        organizer.setRole(Role.KETUA_DIVISI);

        when(userRepository.findByEmailIgnoreCase("ketua@company.local")).thenReturn(Optional.of(organizer));

        mockMvc.perform(post("/meetings/9/telegram-groups/3/retry")
                        .with(user("ketua@company.local").roles("KETUA_DIVISI"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(meetingService).assertCanRetrySync(organizer, 9L);
        verify(meetingTelegramNotificationService).retry(9L, 3L);
    }
}
