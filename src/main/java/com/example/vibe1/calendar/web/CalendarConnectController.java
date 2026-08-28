package com.example.vibe1.calendar.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Landing page for the "google-calendar" registration's redirect-uri (see
 * application.properties). OAuth2AuthorizationCodeGrantFilter (registered via
 * .oauth2Client() in SecurityConfig) has already exchanged the code and saved
 * the OAuth2AuthorizedClient by the time this controller is reached -- it
 * only needs to send the user somewhere sensible afterward.
 */
@Controller
public class CalendarConnectController {

    @GetMapping("/connect/google-calendar")
    public String connected() {
        return "redirect:/";
    }
}
