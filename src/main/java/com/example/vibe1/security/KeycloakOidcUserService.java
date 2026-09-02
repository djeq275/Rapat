package com.example.vibe1.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.vibe1.user.Role;
import com.example.vibe1.user.User;
import com.example.vibe1.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Maps a successful Keycloak login to a local account by email -- unlike
 * {@link GoogleOidcUserService}, this is the one login path that
 * auto-provisions a new {@code User} (role {@code KARYAWAN}, no division)
 * when the email isn't registered yet, since every employee already exists
 * in the office's Keycloak realm (see PRD US-1). Admin assigns the real
 * division/role afterward through the existing user-management page.
 */
@Service
@RequiredArgsConstructor
class KeycloakOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final UserRepository userRepository;
    private final OidcUserService delegate;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        String email = oidcUser.getEmail();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> provisionNewUser(email, oidcUser));

        return new UserPrincipal(user, oidcUser);
    }

    private User provisionNewUser(String email, OidcUser oidcUser) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(oidcUser.getFullName() != null ? oidcUser.getFullName() : email);
        user.setRole(Role.KARYAWAN);
        user.setEnabled(true);
        return userRepository.save(user);
    }
}
