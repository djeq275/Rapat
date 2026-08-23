package com.example.vibe1.security;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.vibe1.user.Role;
import com.example.vibe1.user.User;
import com.example.vibe1.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    UserRepository userRepository;

    AppUserDetailsService service;

    @Test
    void loadsUserWithPasswordHash() {
        service = new AppUserDetailsService(userRepository);
        User user = new User();
        user.setEmail("ketua@company.local");
        user.setPasswordHash("hashed");
        user.setFullName("Ketua Divisi");
        user.setRole(Role.KETUA_DIVISI);
        when(userRepository.findByEmailIgnoreCase("ketua@company.local")).thenReturn(Optional.of(user));

        UserPrincipal principal = (UserPrincipal) service.loadUserByUsername("ketua@company.local");

        assertThat(principal.getUsername()).isEqualTo("ketua@company.local");
        assertThat(principal.getPassword()).isEqualTo("hashed");
    }

    @Test
    void rejectsUnknownEmail() {
        service = new AppUserDetailsService(userRepository);
        when(userRepository.findByEmailIgnoreCase("unknown@company.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown@company.local"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void rejectsGoogleOnlyAccount() {
        service = new AppUserDetailsService(userRepository);
        User user = new User();
        user.setEmail("karyawan@company.local");
        user.setPasswordHash(null);
        user.setFullName("Karyawan");
        user.setRole(Role.KARYAWAN);
        when(userRepository.findByEmailIgnoreCase("karyawan@company.local")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.loadUserByUsername("karyawan@company.local"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
