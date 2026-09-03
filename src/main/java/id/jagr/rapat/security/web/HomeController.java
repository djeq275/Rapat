package id.jagr.rapat.security.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import id.jagr.rapat.calendar.CalendarConsentStatus;
import id.jagr.rapat.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CalendarConsentStatus calendarConsentStatus;

    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("principal", principal);
        model.addAttribute("needsCalendarConsent", calendarConsentStatus.needsConsent(principal.getUsername()));
        return "home";
    }
}
