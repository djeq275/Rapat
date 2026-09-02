package com.example.vibe1.security.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.vibe1.security.KeycloakConfigService;

import lombok.RequiredArgsConstructor;

/** The client secret is never redisplayed once saved -- only whether one is currently set. */
@Controller
@RequestMapping("/admin/keycloak/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class KeycloakSettingsController {

    private final KeycloakConfigService keycloakConfigService;

    @GetMapping
    public String edit(Model model) {
        KeycloakSettingsForm form = new KeycloakSettingsForm();
        keycloakConfigService.currentConfig().ifPresent(config -> {
            form.setServerUrl(config.getServerUrl());
            form.setRealm(config.getRealm());
            form.setClientId(config.getClientId());
        });
        model.addAttribute("form", form);
        model.addAttribute("configured", keycloakConfigService.isConfigured());
        return "admin/keycloak-settings";
    }

    @PostMapping
    public String update(@ModelAttribute("form") KeycloakSettingsForm form, BindingResult bindingResult, Model model) {
        try {
            keycloakConfigService.save(form.getServerUrl(), form.getRealm(), form.getClientId(), form.getClientSecret());
            return "redirect:/admin/keycloak/settings";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("error", ex.getMessage());
            model.addAttribute("configured", keycloakConfigService.isConfigured());
            return "admin/keycloak-settings";
        }
    }
}
