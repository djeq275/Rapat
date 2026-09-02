package com.example.vibe1.telegram.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.vibe1.telegram.TelegramGroup;
import com.example.vibe1.telegram.TelegramGroupService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/telegram-groups")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TelegramGroupAdminController {

    private final TelegramGroupService telegramGroupService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("groups", telegramGroupService.findAll());
        return "admin/telegram-group-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new TelegramGroupForm());
        return "admin/telegram-group-form";
    }

    @PostMapping
    public String create(@ModelAttribute("form") TelegramGroupForm form, BindingResult bindingResult) {
        try {
            telegramGroupService.create(form.getName(), form.getChatId());
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("error", ex.getMessage());
        }
        return bindingResult.hasErrors() ? "admin/telegram-group-form" : "redirect:/admin/telegram-groups";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TelegramGroup group = telegramGroupService.findById(id);
        TelegramGroupForm form = new TelegramGroupForm();
        form.setName(group.getName());
        form.setChatId(group.getChatId());
        form.setEnabled(group.isEnabled());
        model.addAttribute("form", form);
        model.addAttribute("groupId", id);
        return "admin/telegram-group-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("form") TelegramGroupForm form, BindingResult bindingResult, Model model) {
        try {
            telegramGroupService.update(id, form.getName(), form.getChatId(), form.isEnabled());
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("error", ex.getMessage());
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("groupId", id);
            return "admin/telegram-group-form";
        }
        return "redirect:/admin/telegram-groups";
    }
}
