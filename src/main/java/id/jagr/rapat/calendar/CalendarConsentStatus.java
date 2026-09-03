package id.jagr.rapat.calendar;

import org.springframework.stereotype.Service;

import id.jagr.rapat.user.User;
import id.jagr.rapat.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Whether a user still needs to grant the "google-calendar" scope. Only
 * organizer-capable roles (Ketua Divisi/Admin) are ever asked -- everyone
 * else just receives Calendar invites as attendees, no consent needed.
 */
@Service
@RequiredArgsConstructor
public class CalendarConsentStatus {

    private final UserRepository userRepository;
    private final GoogleAuthorizedClientService googleAuthorizedClientService;

    public boolean needsConsent(String email) {
        boolean organizerCapable = userRepository.findByEmailIgnoreCase(email)
                .map(User::isOrganizerCapable)
                .orElse(false);
        if (!organizerCapable) {
            return false;
        }
        return googleAuthorizedClientService.loadAuthorizedClient(
                RegistrationRoutingAuthorizedClientRepository.PERSISTENT_REGISTRATION_ID, email) == null;
    }
}
