package com.example.vibe1.meeting.web;

import java.security.Principal;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.vibe1.meeting.CreateMeetingCommand;
import com.example.vibe1.meeting.Meeting;
import com.example.vibe1.meeting.MeetingAccessService;
import com.example.vibe1.meeting.MeetingParticipant;
import com.example.vibe1.meeting.MeetingParticipantRepository;
import com.example.vibe1.meeting.MeetingRepository;
import com.example.vibe1.meeting.MeetingService;
import com.example.vibe1.meeting.MeetingSyncAuthorizationPort;
import com.example.vibe1.meeting.MeetingTimeZone;
import com.example.vibe1.meeting.NotetakerService;
import com.example.vibe1.meeting.NotulensiAccessService;
import com.example.vibe1.telegram.DivisionTelegramGroupService;
import com.example.vibe1.telegram.MeetingTelegramNotificationService;
import com.example.vibe1.telegram.TelegramGroupService;
import com.example.vibe1.user.Role;
import com.example.vibe1.user.User;
import com.example.vibe1.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Deliberately depends only on {@code java.security.Principal} + the user
 * module's {@code User}/{@code UserRepository}, never on the security
 * module's {@code UserPrincipal} -- security already depends on calendar
 * (consent banner) which depends on meeting, so meeting -> security would
 * close a module cycle.
 */
@Controller
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;
    private final MeetingAccessService meetingAccessService;
    private final NotulensiAccessService notulensiAccessService;
    private final MeetingSyncAuthorizationPort meetingSyncAuthorizationPort;
    private final NotetakerService notetakerService;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final TelegramGroupService telegramGroupService;
    private final DivisionTelegramGroupService divisionTelegramGroupService;
    private final MeetingTelegramNotificationService meetingTelegramNotificationService;

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("meetings", meetingAccessService.visibleMeetings(currentUser(principal)));
        return "meeting/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        User viewer = currentUser(principal);
        Meeting meeting = meetingRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rapat tidak ditemukan"));
        meetingAccessService.assertCanView(viewer, meeting);

        List<MeetingParticipant> participants = participantRepository.findByMeetingId(id);
        boolean canManageNotetakers = notulensiAccessService.canManageNotetakers(viewer, meeting);

        model.addAttribute("meeting", meeting);
        model.addAttribute("participants", participants);
        model.addAttribute("notetakers", notetakerService.findNotetakers(id));
        model.addAttribute("notulensiForm", new NotulensiForm());
        model.addAttribute("canWriteNotulensi", notulensiAccessService.canWrite(viewer, meeting));
        model.addAttribute("canManageNotetakers", canManageNotetakers);
        model.addAttribute("canRetrySync", meetingSyncAuthorizationPort.canRetrySync(viewer, id));
        if (canManageNotetakers) {
            model.addAttribute("notetakerCandidates", notetakerService.findEligibleCandidates(id));
        }
        model.addAttribute("telegramNotifications", meetingTelegramNotificationService.findByMeetingId(id));
        // Same rule as calendar retry -- organizer or Admin.
        model.addAttribute("canRetryTelegram", meetingSyncAuthorizationPort.canRetrySync(viewer, id));
        return "meeting/detail";
    }

    @PostMapping("/{id}/telegram-groups/{groupId}/retry")
    public String retryTelegramNotification(@PathVariable Long id, @PathVariable Long groupId, Principal principal) {
        meetingSyncAuthorizationPort.assertCanRetrySync(currentUser(principal), id);
        meetingTelegramNotificationService.retry(id, groupId);
        return "redirect:/meetings/" + id;
    }

    @PostMapping("/{id}/notulensi")
    public String updateNotulensi(@PathVariable Long id, Principal principal,
                                   @ModelAttribute("notulensiForm") NotulensiForm form) {
        Meeting meeting = meetingRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rapat tidak ditemukan"));
        notulensiAccessService.assertCanWrite(currentUser(principal), meeting);
        meeting.setNotulensiLink(form.getNotulensiLink());
        meeting.setNotulensiText(form.getNotulensiText());
        meetingRepository.save(meeting);
        return "redirect:/meetings/" + id;
    }

    @PostMapping("/{id}/notetakers")
    public String assignNotetaker(@PathVariable Long id, Principal principal, @RequestParam Long userId) {
        User manager = currentUser(principal);
        Meeting meeting = meetingRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rapat tidak ditemukan"));
        notulensiAccessService.assertCanManageNotetakers(manager, meeting);
        notetakerService.assign(meeting, userId, manager.getId());
        return "redirect:/meetings/" + id;
    }

    @PostMapping("/{id}/notetakers/{userId}/revoke")
    public String revokeNotetaker(@PathVariable Long id, @PathVariable Long userId, Principal principal) {
        Meeting meeting = meetingRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rapat tidak ditemukan"));
        notulensiAccessService.assertCanManageNotetakers(currentUser(principal), meeting);
        notetakerService.revoke(id, userId);
        return "redirect:/meetings/" + id;
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('KETUA_DIVISI')")
    public String newForm(Principal principal, Model model) {
        User organizer = currentUser(principal);
        MeetingForm form = new MeetingForm();
        form.setTelegramGroupIds(List.copyOf(divisionTelegramGroupService.findFavoriteGroupIds(organizer.getDivision().getId())));
        model.addAttribute("form", form);
        addCandidates(organizer, model);
        return "meeting/form";
    }

    @PostMapping
    @PreAuthorize("hasRole('KETUA_DIVISI')")
    public String create(Principal principal, @ModelAttribute("form") MeetingForm form,
                          BindingResult bindingResult, Model model) {
        User organizer = currentUser(principal);
        try {
            CreateMeetingCommand command = new CreateMeetingCommand(
                    form.getTitle(),
                    form.getDescription(),
                    form.getMaterialLink(),
                    form.getStartTime().atZone(MeetingTimeZone.WIB).toInstant(),
                    form.getEndTime().atZone(MeetingTimeZone.WIB).toInstant(),
                    organizer.getDivision().getId(),
                    organizer.getId(),
                    form.getParticipantUserIds(),
                    form.getTelegramGroupIds());
            Meeting meeting = meetingService.create(command);
            return "redirect:/meetings/" + meeting.getId();
        } catch (IllegalArgumentException | NullPointerException ex) {
            bindingResult.reject("error", ex.getMessage());
            addCandidates(organizer, model);
            return "meeting/form";
        }
    }

    private User currentUser(Principal principal) {
        return userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Pengguna tidak ditemukan: " + principal.getName()));
    }

    private void addCandidates(User organizer, Model model) {
        model.addAttribute("candidates", userRepository.findByDivisionIdAndRole(organizer.getDivision().getId(), Role.KARYAWAN));
        model.addAttribute("telegramGroups", telegramGroupService.findActive());
    }
}
