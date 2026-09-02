package com.example.vibe1.telegram.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.vibe1.division.DivisionRepository;
import com.example.vibe1.security.GoogleOidcUserService;
import com.example.vibe1.security.SecurityConfig;
import com.example.vibe1.telegram.DivisionTelegramGroupService;
import com.example.vibe1.telegram.TelegramGroupService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DivisionTelegramGroupAdminController.class)
@Import(SecurityConfig.class)
class DivisionTelegramGroupAdminControllerTest {

    @MockitoBean
    DivisionRepository divisionRepository;
    @MockitoBean
    TelegramGroupService telegramGroupService;
    @MockitoBean
    DivisionTelegramGroupService divisionTelegramGroupService;
    @MockitoBean
    GoogleOidcUserService googleOidcUserService;

    @Autowired
    MockMvc mockMvc;

    @Test
    void nonAdminGets403() throws Exception {
        mockMvc.perform(get("/admin/divisions/1/telegram-groups").with(user("karyawan@company.local").roles("KARYAWAN")))
                .andExpect(status().isForbidden());
    }
}
