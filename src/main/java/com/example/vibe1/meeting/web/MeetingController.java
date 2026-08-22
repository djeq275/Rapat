package com.example.vibe1.meeting.web;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.vibe1.meeting.CreateMeetingCommand;
import com.example.vibe1.meeting.Meeting;
import com.example.vibe1.meeting.MeetingService;
import com.example.vibe1.meeting.MeetingTimeZone;
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
    private final UserRepository userRepository;

    @GetMapping("/new")
    @PreAuthorize("hasRole('KETUA_DIVISI')")
    public String newForm(Principal principal, Model model) {
        model.addAttribute("form", new MeetingForm());
        addCandidates(currentUser(principal), model);
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
                    form.getParticipantUserIds());
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
    }
}
