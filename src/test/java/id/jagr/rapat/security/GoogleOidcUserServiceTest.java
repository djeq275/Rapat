package id.jagr.rapat.security;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import id.jagr.rapat.user.Role;
import id.jagr.rapat.user.User;
import id.jagr.rapat.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleOidcUserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    OidcUserService delegate;

    @Mock
    OidcUserRequest userRequest;

    GoogleOidcUserService service;

    @Test
    void mapsKnownEmailToLocalUser() {
        service = new GoogleOidcUserService(userRepository, delegate);
        OidcUser googleUser = stubOidcUser("direktur@company.local");
        when(delegate.loadUser(userRequest)).thenReturn(googleUser);

        User user = new User();
        user.setEmail("direktur@company.local");
        user.setFullName("Direktur");
        user.setRole(Role.DIREKTUR);
        user.setEnabled(true);
        when(userRepository.findByEmailIgnoreCase("direktur@company.local")).thenReturn(Optional.of(user));

        OidcUser result = service.loadUser(userRequest);

        assertThat(result).isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) result).getRole()).isEqualTo(Role.DIREKTUR);
    }

    @Test
    void rejectsUnregisteredEmailWithoutCreatingAccount() {
        service = new GoogleOidcUserService(userRepository, delegate);
        OidcUser googleUser = stubOidcUser("stranger@gmail.com");
        when(delegate.loadUser(userRequest)).thenReturn(googleUser);
        when(userRepository.findByEmailIgnoreCase("stranger@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(userRepository, never()).save(any());
    }

    private OidcUser stubOidcUser(String email) {
        DefaultOidcUser oidcUser = mock(DefaultOidcUser.class);
        org.mockito.Mockito.lenient().when(oidcUser.getEmail()).thenReturn(email);
        org.mockito.Mockito.lenient().when(oidcUser.getAttributes()).thenReturn(java.util.Map.of("email", email));
        return oidcUser;
    }
}
