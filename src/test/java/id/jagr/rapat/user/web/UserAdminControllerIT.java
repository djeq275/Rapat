package id.jagr.rapat.user.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import id.jagr.rapat.division.Division;
import id.jagr.rapat.division.DivisionRepository;
import id.jagr.rapat.user.Role;
import id.jagr.rapat.user.User;
import id.jagr.rapat.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    Division division;
    Long ketuaId;

    @BeforeEach
    void seed() {
        division = divisionRepository.save(new Division("Engineering IT " + System.nanoTime()));
        User ketua = new User();
        ketua.setEmail("ketua-" + System.nanoTime() + "@company.local");
        ketua.setFullName("Ketua Divisi Test");
        ketua.setRole(Role.KETUA_DIVISI);
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
}
