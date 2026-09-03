package com.example.vibe1.security.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.vibe1.security.AppOidcUserService;
import com.example.vibe1.security.KeycloakConfigService;
import com.example.vibe1.security.SecurityConfig;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The "Login dengan Keycloak" option must only appear once Admin has actually configured a connection. */
@WebMvcTest(AuthWebController.class)
@Import(SecurityConfig.class)
class AuthWebControllerTest {

    @MockitoBean
    KeycloakConfigService keycloakConfigService;
    @MockitoBean
    AppOidcUserService appOidcUserService;
    @MockitoBean
    ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    MockMvc mockMvc;

    @Test
    void keycloakButtonHiddenWhenNotConfigured() throws Exception {
        when(keycloakConfigService.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Login dengan Keycloak"))));
    }

    @Test
    void keycloakButtonShownWhenConfigured() throws Exception {
        when(keycloakConfigService.isConfigured()).thenReturn(true);

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Login dengan Keycloak")));
    }
}
