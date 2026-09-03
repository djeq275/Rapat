package com.example.vibe1.security.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.vibe1.security.GoogleOAuthConfigService;

import lombok.RequiredArgsConstructor;

/** The client secret is never redisplayed once saved -- only whether one is currently set. */
@Controller
@RequestMapping("/admin/google-oauth/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class GoogleOAuthSettingsController {

    private final GoogleOAuthConfigService googleOAuthConfigService;

    @GetMapping
    public String edit(Model model) {
        GoogleOAuthSettingsForm form = new GoogleOAuthSettingsForm();
        googleOAuthConfigService.currentConfig().ifPresent(config -> form.setClientId(config.getClientId()));
        model.addAttribute("form", form);
        model.addAttribute("configured", googleOAuthConfigService.isConfigured());
        return "admin/google-oauth-settings";
    }

    @PostMapping
    public String update(@ModelAttribute("form") GoogleOAuthSettingsForm form, BindingResult bindingResult, Model model) {
        try {
            googleOAuthConfigService.save(form.getClientId(), form.getClientSecret());
            return "redirect:/admin/google-oauth/settings";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("error", ex.getMessage());
            model.addAttribute("configured", googleOAuthConfigService.isConfigured());
            return "admin/google-oauth-settings";
        }
    }
}
