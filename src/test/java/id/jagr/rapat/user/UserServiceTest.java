package id.jagr.rapat.user;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import id.jagr.rapat.division.Division;
import id.jagr.rapat.division.DivisionRepository;
import id.jagr.rapat.division.DivisionService;
import id.jagr.rapat.user.web.UserForm;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    DivisionRepository divisionRepository;
    @Mock
    DivisionService divisionService;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    AppRoleRepository appRoleRepository;

    UserService service;

    @Test
    void karyawanWithoutDivisionIsRejected() {
        service = new UserService(userRepository, divisionRepository, divisionService, passwordEncoder, appRoleRepository);
        lenient().when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        AppRole karyawan = AppRoleFixtures.karyawan();
        karyawan.setId(4L);
        when(appRoleRepository.findById(4L)).thenReturn(Optional.of(karyawan));

        UserForm form = new UserForm();
        form.setEmail("karyawan@company.local");
        form.setFullName("Karyawan");
        form.setRoleId(4L);
        form.setDivisionId(null);

        assertThatThrownBy(() -> service.create(form)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adminWithDivisionIsRejected() {
        service = new UserService(userRepository, divisionRepository, divisionService, passwordEncoder, appRoleRepository);
        lenient().when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        AppRole admin = AppRoleFixtures.admin();
        admin.setId(1L);
        when(appRoleRepository.findById(1L)).thenReturn(Optional.of(admin));

        UserForm form = new UserForm();
        form.setEmail("admin2@company.local");
        form.setFullName("Admin Dua");
        form.setRoleId(1L);
        form.setDivisionId(1L);

        assertThatThrownBy(() -> service.create(form)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void divisionLeaderFlagRejectedForNonOrganizerRole() {
        service = new UserService(userRepository, divisionRepository, divisionService, passwordEncoder, appRoleRepository);
        lenient().when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        AppRole karyawan = AppRoleFixtures.karyawan();
        karyawan.setId(4L);
        when(appRoleRepository.findById(4L)).thenReturn(Optional.of(karyawan));

        UserForm form = new UserForm();
        form.setEmail("karyawan2@company.local");
        form.setFullName("Karyawan Dua");
        form.setRoleId(4L);
        form.setDivisionId(1L);
        form.setDivisionLeader(true);

        assertThatThrownBy(() -> service.create(form)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void movingUserOutOfDivisionClearsOldLeadership() {
        service = new UserService(userRepository, divisionRepository, divisionService, passwordEncoder, appRoleRepository);

        Division oldDivision = new Division("Old");
        oldDivision.setId(1L);
        Division newDivision = new Division("New");
        newDivision.setId(2L);

        AppRole ketuaDivisi = AppRoleFixtures.ketuaDivisi();
        ketuaDivisi.setId(3L);
        when(appRoleRepository.findById(3L)).thenReturn(Optional.of(ketuaDivisi));

        User user = new User();
        user.setId(10L);
        user.setEmail("ketua@company.local");
        user.setFullName("Ketua");
        user.setRole(ketuaDivisi);
        user.setDivision(oldDivision);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCase("ketua@company.local")).thenReturn(Optional.of(user));
        when(divisionRepository.findById(2L)).thenReturn(Optional.of(newDivision));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserForm form = new UserForm();
        form.setEmail("ketua@company.local");
        form.setFullName("Ketua");
        form.setRoleId(3L);
        form.setDivisionId(2L);
        form.setDivisionLeader(true);

        service.update(10L, form);

        verify(divisionService).clearKetuaIfMatches(1L, 10L);
        verify(divisionService).setKetua(2L, 10L);
    }
}
