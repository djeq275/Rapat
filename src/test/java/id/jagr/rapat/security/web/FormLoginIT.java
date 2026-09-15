package id.jagr.rapat.security.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack regression test for a real Hibernate session, not a mocked repository: a
 * @WebMvcTest slice can never catch LazyInitializationException on User.role (LAZY since
 * issue #45) because Mockito never returns a real lazy proxy. This exercises the actual
 * DaoAuthenticationProvider -> AppUserDetailsService -> UserPrincipal.getAuthorities() path
 * against a real MariaDB session using the seeded bootstrap admin account (issue #35 notes
 * this credential is public -- fine for a throwaway test container).
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class FormLoginIT {

    @Container
    @ServiceConnection
    static final MariaDBContainer<?> mariaDb = new MariaDBContainer<>("mariadb:11");

    @Autowired
    MockMvc mockMvc;

    @Test
    void bootstrapAdminCanLogIn() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "admin@company.local")
                        .param("password", "ChangeMe123!")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}
