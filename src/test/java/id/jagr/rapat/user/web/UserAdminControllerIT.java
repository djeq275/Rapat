package id.jagr.rapat.user.web;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import id.jagr.rapat.division.Division;
import id.jagr.rapat.division.DivisionRepository;
import id.jagr.rapat.user.AppRole;
import id.jagr.rapat.user.AppRoleRepository;
import id.jagr.rapat.user.BuiltInRoleNames;
import id.jagr.rapat.user.Capability;
import id.jagr.rapat.user.CapabilityRepository;
import id.jagr.rapat.user.User;
import id.jagr.rapat.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test: rendering a user with a division outside the repository
 * call's own transaction (open-in-view is disabled) must not throw
 * LazyInitializationException. Needs a real Hibernate session/DB, so this is
 * a full @SpringBootTest against a real container, not a @WebMvcTest slice
 * (mocked services would return plain POJOs and could never reproduce this).
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class UserAdminControllerIT {

    @Container
    @ServiceConnection
    static final MariaDBContainer<?> mariaDb = new MariaDBContainer<>("mariadb:11");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    DivisionRepository divisionRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AppRoleRepository appRoleRepository;
    @Autowired
    CapabilityRepository capabilityRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    Division division;
    Long ketuaId;

    @BeforeEach
    void seed() {
        division = divisionRepository.save(new Division("Engineering IT " + System.nanoTime()));
        User ketua = new User();
        ketua.setEmail("ketua-" + System.nanoTime() + "@company.local");
        ketua.setFullName("Ketua Divisi Test");
        ketua.setRole(appRoleRepository.findByNameIgnoreCase(BuiltInRoleNames.KETUA_DIVISI).orElseThrow());
        ketua.setDivision(division);
        ketuaId = userRepository.save(ketua).getId();
    }

    @Test
    void listRendersDivisionNameWithoutLazyInitializationException() throws Exception {
        mockMvc.perform(get("/admin/users").with(user("admin@test.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(division.getName())));
    }

    @Test
    void editFormRendersDivisionWithoutLazyInitializationException() throws Exception {
        mockMvc.perform(get("/admin/users/{id}/edit", ketuaId)
                        .with(user("admin@test.local").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    /**
     * End-to-end through the real pipeline (UserRepository's eager fetch -> UserPrincipal
     * .getAuthorities() -> @PreAuthorize) rather than MockMvc's .with(user(...).authorities(...))
     * shortcut, which never touches any of that production code (issue #47).
     */
    @Test
    void customRoleWithMatchingCapabilityCanLogInAndAccessUserAdmin() throws Exception {
        Capability manageUsers = capabilityRepository.findAll().stream()
                .filter(c -> "MANAGE_USERS".equals(c.getCode()))
                .findFirst().orElseThrow();
        AppRole customRole = new AppRole("Manajer SDM " + System.nanoTime(), false, false, false, false, false);
        customRole.setCapabilities(Set.of(manageUsers));
        customRole = appRoleRepository.save(customRole);

        String email = "manajer-sdm-" + System.nanoTime() + "@company.local";
        User manajer = new User();
        manajer.setEmail(email);
        manajer.setFullName("Manajer SDM Test");
        manajer.setPasswordHash(passwordEncoder.encode("TestPass123!"));
        manajer.setRole(customRole);
        userRepository.save(manajer);

        MockHttpSession session = logIn(email, "TestPass123!");

        mockMvc.perform(get("/admin/users").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void customRoleWithoutMatchingCapabilityStays403ForUserAdmin() throws Exception {
        AppRole customRole = appRoleRepository.save(
                new AppRole("Peninjau Test " + System.nanoTime(), false, false, false, true, false));

        String email = "peninjau-" + System.nanoTime() + "@company.local";
        User peninjau = new User();
        peninjau.setEmail(email);
        peninjau.setFullName("Peninjau Test");
        peninjau.setPasswordHash(passwordEncoder.encode("TestPass123!"));
        peninjau.setRole(customRole);
        userRepository.save(peninjau);

        MockHttpSession session = logIn(email, "TestPass123!");

        mockMvc.perform(get("/admin/users").session(session))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession logIn(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("username", email)
                        .param("password", password)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) loginResult.getRequest().getSession();
    }
}
