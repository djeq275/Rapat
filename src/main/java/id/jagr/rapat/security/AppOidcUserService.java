package id.jagr.rapat.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * {@code oauth2Login().userInfoEndpoint().oidcUserService(...)} only accepts
 * one bean, shared across every OIDC registration -- this dispatches by
 * registration id so each identity provider keeps its own account-matching
 * rules: {@link GoogleOidcUserService} (never auto-creates) is untouched for
 * every registration id except {@value DynamicClientRegistrationRepository#KEYCLOAK_REGISTRATION_ID},
 * which goes to {@link KeycloakOidcUserService} (auto-provisions).
 */
@Component
@RequiredArgsConstructor
public class AppOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final GoogleOidcUserService googleOidcUserService;
    private final KeycloakOidcUserService keycloakOidcUserService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (DynamicClientRegistrationRepository.KEYCLOAK_REGISTRATION_ID.equals(registrationId)) {
            return keycloakOidcUserService.loadUser(userRequest);
        }
        return googleOidcUserService.loadUser(userRequest);
    }
}
