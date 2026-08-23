package com.example.vibe1.calendar.web;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.vibe1.calendar.CalendarSyncService;
import com.example.vibe1.meeting.MeetingSyncAuthorizationPort;
import com.example.vibe1.user.User;
import com.example.vibe1.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lives in the calendar module (not meeting.web) even though its route is
 * nested under /meetings/** -- meeting never depends on calendar, so the
 * retry action has to be served from this side of that one-way edge.
 */
@Controller
@RequiredArgsConstructor
public class CalendarSyncController {

    private final CalendarSyncService calendarSyncService;
    private final MeetingSyncAuthorizationPort meetingSyncAuthorizationPort;
    private final UserRepository userRepository;

    @PostMapping("/meetings/{id}/retry-sync")
    public String retry(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Pengguna tidak ditemukan: " + principal.getName()));
        meetingSyncAuthorizationPort.assertCanRetrySync(user, id);
        calendarSyncService.sync(id);
        redirectAttributes.addFlashAttribute("message", "Sync ulang dijalankan.");
        return "redirect:/meetings/" + id;
    }
}
