package id.jagr.rapat.calendar;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

import id.jagr.rapat.meeting.MeetingSyncData;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;

@Component
class GoogleCalendarGatewayImpl implements GoogleCalendarGateway {

    private static final String REGISTRATION_ID = RegistrationRoutingAuthorizedClientRepository.PERSISTENT_REGISTRATION_ID;

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    GoogleCalendarGatewayImpl(OAuth2AuthorizedClientManager backgroundAuthorizedClientManager) {
        this.authorizedClientManager = backgroundAuthorizedClientManager;
    }

    @Override
    public String insertEvent(MeetingSyncData data) {
        String accessToken = resolveAccessToken(data.organizerEmail());

        Event event = new Event()
                .setSummary(data.title())
                .setDescription(buildDescription(data))
                .setStart(new EventDateTime().setDateTime(toGoogleDateTime(data.startTime())))
                .setEnd(new EventDateTime().setDateTime(toGoogleDateTime(data.endTime())))
                .setAttendees(data.participantEmails().stream()
                        .map(email -> new EventAttendee().setEmail(email))
                        .toList());

        try {
            Calendar calendar = buildClient(accessToken);
            Event created = calendar.events().insert("primary", event)
                    // Without this, Calendar can create the event without notifying attendees --
                    // which is the exact bug this whole project exists to fix.
                    .setSendUpdates("all")
                    .execute();
            return created.getId();
        } catch (IOException | GeneralSecurityException e) {
            throw new CalendarSyncException("Gagal membuat event Google Calendar: " + e.getMessage(), e);
        }
    }

    private String resolveAccessToken(String organizerEmail) {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                .principal(organizerEmail)
                .build();
        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(request);
        if (authorizedClient == null) {
            throw new CalendarSyncException("Organizer belum menghubungkan Google Calendar: " + organizerEmail);
        }
        return authorizedClient.getAccessToken().getTokenValue();
    }

    private Calendar buildClient(String accessToken) throws GeneralSecurityException, IOException {
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                httpRequest -> httpRequest.getHeaders().setAuthorization("Bearer " + accessToken))
                .setApplicationName("vibe1")
                .build();
    }

    private String buildDescription(MeetingSyncData data) {
        if (data.materialLink() == null) {
            return data.description();
        }
        String base = data.description() == null ? "" : data.description() + "\n\n";
        return base + "Materi: " + data.materialLink();
    }

    private DateTime toGoogleDateTime(Instant instant) {
        return new DateTime(instant.toEpochMilli());
    }
}
