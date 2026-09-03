package id.jagr.rapat.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import id.jagr.rapat.user.User;
import id.jagr.rapat.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Maps a successful Google login to an existing local account by email.
 * Deliberately never auto-creates a User — accounts are provisioned by an Admin.
 */
@Service
@RequiredArgsConstructor
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final UserRepository userRepository;
    private final OidcUserService delegate;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        String email = oidcUser.getEmail();

        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(User::isEnabled)
                .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error(
                        "account_not_registered",
                        "Akun Google ini tidak terdaftar. Hubungi Admin.",
                        null)));

        return new UserPrincipal(user, oidcUser);
    }
}
