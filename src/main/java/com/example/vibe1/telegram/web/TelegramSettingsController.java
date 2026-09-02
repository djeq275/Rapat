package com.example.vibe1.telegram.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.vibe1.telegram.TelegramBotConfigService;

import lombok.RequiredArgsConstructor;

/** The token is never redisplayed once saved -- only whether one is currently set. */
@Controller
@RequestMapping("/admin/telegram/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TelegramSettingsController {

    private final TelegramBotConfigService telegramBotConfigService;

    @GetMapping
    public String edit(Model model) {
        model.addAttribute("tokenConfigured", telegramBotConfigService.currentToken().isPresent());
        model.addAttribute("form", new TelegramSettingsForm());
        return "admin/telegram-settings";
    }

    @PostMapping
    public String update(@ModelAttribute("form") TelegramSettingsForm form) {
        if (form.getToken() != null && !form.getToken().isBlank()) {
            telegramBotConfigService.save(form.getToken());
        }
        return "redirect:/admin/telegram/settings";
    }
}
