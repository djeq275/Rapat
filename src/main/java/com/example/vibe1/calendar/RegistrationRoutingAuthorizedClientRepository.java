package com.example.vibe1.calendar;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Only "google-calendar" needs durable, encrypted storage (so background sync
 * can refresh it later); "google-login" is identity-only and stays in the
 * default HTTP session store.
 */
class RegistrationRoutingAuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

    static final String PERSISTENT_REGISTRATION_ID = "google-calendar";

    private final OAuth2AuthorizedClientRepository sessionRepository = new HttpSessionOAuth2AuthorizedClientRepository();
    private final GoogleAuthorizedClientService persistentService;

    RegistrationRoutingAuthorizedClientRepository(GoogleAuthorizedClientService persistentService) {
        this.persistentService = persistentService;
    }

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, Authentication principal,
                                                                       HttpServletRequest request) {
        if (PERSISTENT_REGISTRATION_ID.equals(clientRegistrationId)) {
            return persistentService.loadAuthorizedClient(clientRegistrationId, principal.getName());
        }
        return sessionRepository.loadAuthorizedClient(clientRegistrationId, principal, request);
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal,
                                      HttpServletRequest request, HttpServletResponse response) {
        if (PERSISTENT_REGISTRATION_ID.equals(authorizedClient.getClientRegistration().getRegistrationId())) {
            persistentService.saveAuthorizedClient(authorizedClient, principal);
        } else {
            sessionRepository.saveAuthorizedClient(authorizedClient, principal, request, response);
        }
    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId, Authentication principal,
                                        HttpServletRequest request, HttpServletResponse response) {
        if (PERSISTENT_REGISTRATION_ID.equals(clientRegistrationId)) {
            persistentService.removeAuthorizedClient(clientRegistrationId, principal.getName());
        } else {
            sessionRepository.removeAuthorizedClient(clientRegistrationId, principal, request, response);
        }
    }
}
