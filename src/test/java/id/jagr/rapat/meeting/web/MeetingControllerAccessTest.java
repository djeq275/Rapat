package id.jagr.rapat.meeting.web;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import id.jagr.rapat.division.Division;
import id.jagr.rapat.meeting.Meeting;
import id.jagr.rapat.meeting.MeetingAccessService;
import id.jagr.rapat.meeting.MeetingParticipantRepository;
import id.jagr.rapat.meeting.MeetingRepository;
import id.jagr.rapat.meeting.MeetingService;
import id.jagr.rapat.meeting.NotetakerService;
import id.jagr.rapat.meeting.NotulensiAccessService;
import id.jagr.rapat.security.AppOidcUserService;
import id.jagr.rapat.security.SecurityConfig;
import id.jagr.rapat.telegram.DivisionTelegramGroupService;
import id.jagr.rapat.telegram.MeetingTelegramNotificationService;
import id.jagr.rapat.telegram.TelegramGroupService;
import id.jagr.rapat.user.AppRole;
import id.jagr.rapat.user.AppRoleFixtures;
import id.jagr.rapat.user.AppRoleRepository;
import id.jagr.rapat.user.BuiltInRoleNames;
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
    AppRoleRepository appRoleRepository;
    @MockitoBean
    AppOidcUserService appOidcUserService;
    @MockitoBean
    ClientRegistrationRepository clientRegistrationRepository;
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
        karyawan.setRole(AppRoleFixtures.karyawan());

        when(userRepository.findByEmailIgnoreCase("karyawan@company.local")).thenReturn(Optional.of(karyawan));
        when(meetingRepository.findDetailById(5L)).thenReturn(Optional.of(meeting));
        doThrow(new AccessDeniedException("Anda tidak berhak melihat rapat ini"))
                .when(meetingAccessService).assertCanView(eq(karyawan), any());

        mockMvc.perform(get("/meetings/5").with(user("karyawan@company.local").roles("KARYAWAN")))
                .andExpect(status().isForbidden());
    }

    /**
     * The Telegram retry action reuses {@link id.jagr.rapat.meeting.MeetingSyncAuthorizationPort},
     * the same organizer-or-Admin rule as Calendar sync retry -- see {@code CalendarSyncControllerTest}
     * for the analogous coverage on that endpoint.
     */
    @Test
    void telegramRetryDeniedForNonOrganizerSurfacesAs403AndNeverRetries() throws Exception {
        User otherKetua = new User();
        otherKetua.setEmail("ketua-lain@company.local");
        otherKetua.setRole(AppRoleFixtures.ketuaDivisi());

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
        organizer.setRole(AppRoleFixtures.ketuaDivisi());

        when(userRepository.findByEmailIgnoreCase("ketua@company.local")).thenReturn(Optional.of(organizer));

        mockMvc.perform(post("/meetings/9/telegram-groups/3/retry")
                        .with(user("ketua@company.local").roles("KETUA_DIVISI"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(meetingService).assertCanRetrySync(organizer, 9L);
        verify(meetingTelegramNotificationService).retry(9L, 3L);
    }

    /**
     * Admin's AppRole is seeded canOrganizeMeetings=true (issue #45), so its UserPrincipal now
     * carries CAN_ORGANIZE_MEETINGS (issue #53) -- must still not be able to reach the create
     * form, since Admin structurally never has a division. Pins down that this is a clean 403
     * from assertHasDivision(), not a 500 from a null organizer.getDivision().
     */
    @Test
    void adminWithCanOrganizeMeetingsAuthorityStillGets403ForNewMeetingFormDueToNoDivision() throws Exception {
        User admin = new User();
        admin.setEmail("admin@company.local");
        admin.setRole(AppRoleFixtures.admin());
        admin.setDivision(null);

        when(userRepository.findByEmailIgnoreCase("admin@company.local")).thenReturn(Optional.of(admin));

        mockMvc.perform(get("/meetings/new").with(user("admin@company.local")
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("CAN_ORGANIZE_MEETINGS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void customRoleWithoutCanOrganizeMeetingsAuthorityGets403ForNewMeetingForm() throws Exception {
        mockMvc.perform(get("/meetings/new").with(user("peninjau@company.local")
                        .authorities(new SimpleGrantedAuthority("CAPABILITY_MANAGE_DIVISIONS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void customRoleWithCanOrganizeMeetingsAndDivisionCanAccessNewMeetingForm() throws Exception {
        Division division = new Division("Engineering");
        division.setId(1L);
        AppRole manajerRegional = new AppRole("Manajer Regional", false, true, false, false, true);
        User organizer = new User();
        organizer.setEmail("manajer@company.local");
        organizer.setRole(manajerRegional);
        organizer.setDivision(division);

        when(userRepository.findByEmailIgnoreCase("manajer@company.local")).thenReturn(Optional.of(organizer));
        when(divisionTelegramGroupService.findFavoriteGroupIds(1L)).thenReturn(Set.of());
        when(appRoleRepository.findByNameIgnoreCase(BuiltInRoleNames.KARYAWAN)).thenReturn(Optional.of(AppRoleFixtures.karyawan()));
        when(userRepository.findByDivisionIdAndRole(eq(1L), any())).thenReturn(List.of());
        when(telegramGroupService.findActive()).thenReturn(List.of());

        mockMvc.perform(get("/meetings/new").with(user("manajer@company.local")
                        .authorities(new SimpleGrantedAuthority("CAN_ORGANIZE_MEETINGS"))))
                .andExpect(status().isOk());
    }

    @Test
    void customRoleWithCanOrganizeMeetingsButNoDivisionGets403ForNewMeetingForm() throws Exception {
        AppRole manajerRegional = new AppRole("Manajer Regional", false, false, false, false, true);
        User organizer = new User();
        organizer.setEmail("manajer-tanpa-divisi@company.local");
        organizer.setRole(manajerRegional);
        organizer.setDivision(null);

        when(userRepository.findByEmailIgnoreCase("manajer-tanpa-divisi@company.local")).thenReturn(Optional.of(organizer));

        mockMvc.perform(get("/meetings/new").with(user("manajer-tanpa-divisi@company.local")
                        .authorities(new SimpleGrantedAuthority("CAN_ORGANIZE_MEETINGS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminWithCanOrganizeMeetingsAuthorityStillGets403ForCreatePost() throws Exception {
        User admin = new User();
        admin.setEmail("admin@company.local");
        admin.setRole(AppRoleFixtures.admin());
        admin.setDivision(null);

        when(userRepository.findByEmailIgnoreCase("admin@company.local")).thenReturn(Optional.of(admin));

        mockMvc.perform(post("/meetings")
                        .with(user("admin@company.local")
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("CAN_ORGANIZE_MEETINGS")))
                        .with(csrf())
                        .param("title", "Rapat Percobaan")
                        .param("startTime", "2026-01-01T10:00")
                        .param("endTime", "2026-01-01T11:00"))
                .andExpect(status().isForbidden());

        verify(meetingService, never()).create(any());
    }

    @Test
    void customRoleWithCanOrganizeMeetingsAndDivisionCanCreateMeetingViaPost() throws Exception {
        Division division = new Division("Engineering");
        division.setId(1L);
        AppRole manajerRegional = new AppRole("Manajer Regional", false, true, false, false, true);
        User organizer = new User();
        organizer.setId(42L);
        organizer.setEmail("manajer@company.local");
        organizer.setRole(manajerRegional);
        organizer.setDivision(division);

        when(userRepository.findByEmailIgnoreCase("manajer@company.local")).thenReturn(Optional.of(organizer));
        Meeting created = new Meeting();
        created.setId(200L);
        when(meetingService.create(any())).thenReturn(created);

        mockMvc.perform(post("/meetings")
                        .with(user("manajer@company.local").authorities(new SimpleGrantedAuthority("CAN_ORGANIZE_MEETINGS")))
                        .with(csrf())
                        .param("title", "Rapat Regional")
                        .param("startTime", "2026-01-01T10:00")
                        .param("endTime", "2026-01-01T11:00"))
                .andExpect(status().is3xxRedirection());

        verify(meetingService).create(any());
    }
}
