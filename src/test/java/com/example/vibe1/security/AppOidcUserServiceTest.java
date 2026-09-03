package com.example.vibe1.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Proves the dispatcher routes by registration id -- the actual per-provider logic is tested in each service's own test. */
@ExtendWith(MockitoExtension.class)
class AppOidcUserServiceTest {

    @Mock
    GoogleOidcUserService googleOidcUserService;
    @Mock
    KeycloakOidcUserService keycloakOidcUserService;
    @Mock
    OidcUserRequest userRequest;
    @Mock
    ClientRegistration clientRegistration;
    @Mock
    OidcUser oidcUser;

    AppOidcUserService service;

    @Test
    void routesKeycloakRegistrationToKeycloakService() {
        service = new AppOidcUserService(googleOidcUserService, keycloakOidcUserService);
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("keycloak");
        when(keycloakOidcUserService.loadUser(userRequest)).thenReturn(oidcUser);

        service.loadUser(userRequest);

        verify(keycloakOidcUserService).loadUser(userRequest);
        verify(googleOidcUserService, never()).loadUser(any());
    }

    @Test
    void routesEveryOtherRegistrationToGoogleService() {
        service = new AppOidcUserService(googleOidcUserService, keycloakOidcUserService);
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("google-login");
        when(googleOidcUserService.loadUser(userRequest)).thenReturn(oidcUser);

        service.loadUser(userRequest);

        verify(googleOidcUserService).loadUser(userRequest);
        verify(keycloakOidcUserService, never()).loadUser(any());
    }
}
