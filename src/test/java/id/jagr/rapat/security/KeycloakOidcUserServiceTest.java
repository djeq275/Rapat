package id.jagr.rapat.security;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import id.jagr.rapat.user.Role;
import id.jagr.rapat.user.User;
import id.jagr.rapat.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakOidcUserServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    OidcUserService delegate;
    @Mock
    OidcUserRequest userRequest;

    KeycloakOidcUserService service;

    @Test
    void autoProvisionsNewAccountForUnregisteredEmail() {
        service = new KeycloakOidcUserService(userRepository, delegate);
        OidcUser oidcUser = stubOidcUser("karyawan.baru@company.local", "Karyawan Baru");
        when(delegate.loadUser(userRequest)).thenReturn(oidcUser);
        when(userRepository.findByEmailIgnoreCase("karyawan.baru@company.local")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OidcUser result = service.loadUser(userRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("karyawan.baru@company.local");
        assertThat(captor.getValue().getFullName()).isEqualTo("Karyawan Baru");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.KARYAWAN);
        assertThat(captor.getValue().getDivision()).isNull();
        assertThat(captor.getValue().isEnabled()).isTrue();

        assertThat(result).isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) result).getRole()).isEqualTo(Role.KARYAWAN);
    }

    @Test
    void fallsBackToEmailAsFullNameWhenNameClaimMissing() {
        service = new KeycloakOidcUserService(userRepository, delegate);
        DefaultOidcUser oidcUser = mock(DefaultOidcUser.class);
        lenient().when(oidcUser.getEmail()).thenReturn("noname@company.local");
        lenient().when(oidcUser.getAttributes()).thenReturn(Map.of("email", "noname@company.local"));
        lenient().when(oidcUser.getFullName()).thenReturn(null);
        when(delegate.loadUser(userRequest)).thenReturn(oidcUser);
        when(userRepository.findByEmailIgnoreCase("noname@company.local")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.loadUser(userRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFullName()).isEqualTo("noname@company.local");
    }

    @Test
    void reusesExistingAccountForKnownEmailWithoutCreatingDuplicate() {
        service = new KeycloakOidcUserService(userRepository, delegate);
        OidcUser oidcUser = stubOidcUser("ketua@company.local", "Ketua Existing");
        when(delegate.loadUser(userRequest)).thenReturn(oidcUser);

        User existing = new User();
        existing.setId(42L);
        existing.setEmail("ketua@company.local");
        existing.setFullName("Ketua Existing");
        existing.setRole(Role.KETUA_DIVISI);
        existing.setEnabled(true);
        when(userRepository.findByEmailIgnoreCase("ketua@company.local")).thenReturn(Optional.of(existing));

        OidcUser result = service.loadUser(userRequest);

        verify(userRepository, never()).save(any());
        assertThat(((UserPrincipal) result).getRole()).isEqualTo(Role.KETUA_DIVISI);
    }

    private OidcUser stubOidcUser(String email, String fullName) {
        DefaultOidcUser oidcUser = mock(DefaultOidcUser.class);
        lenient().when(oidcUser.getEmail()).thenReturn(email);
        lenient().when(oidcUser.getAttributes()).thenReturn(Map.of("email", email, "name", fullName));
        lenient().when(oidcUser.getFullName()).thenReturn(fullName);
        return oidcUser;
    }
}
