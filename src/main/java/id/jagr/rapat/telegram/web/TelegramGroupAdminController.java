package id.jagr.rapat.telegram.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import id.jagr.rapat.telegram.TelegramBotConfigService;
import id.jagr.rapat.telegram.TelegramChatCandidate;
import id.jagr.rapat.telegram.TelegramGateway;
import id.jagr.rapat.telegram.TelegramGroup;
import id.jagr.rapat.telegram.TelegramGroupService;
import id.jagr.rapat.telegram.TelegramSendException;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/telegram-groups")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('CAPABILITY_MANAGE_TELEGRAM_GROUPS')")
@RequiredArgsConstructor
public class TelegramGroupAdminController {

    private final TelegramGroupService telegramGroupService;
    private final TelegramBotConfigService telegramBotConfigService;
    private final TelegramGateway telegramGateway;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("groups", telegramGroupService.findAll());
        return "admin/telegram-group-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new TelegramGroupForm());
        addChatCandidates(model);
        return "admin/telegram-group-form";
    }

    @PostMapping
    public String create(@ModelAttribute("form") TelegramGroupForm form, BindingResult bindingResult, Model model) {
        try {
            telegramGroupService.create(form.getName(), form.getChatId());
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("error", ex.getMessage());
        }
        if (bindingResult.hasErrors()) {
            addChatCandidates(model);
            return "admin/telegram-group-form";
        }
        return "redirect:/admin/telegram-groups";
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
        addChatCandidates(model);
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
            addChatCandidates(model);
            return "admin/telegram-group-form";
        }
        return "redirect:/admin/telegram-groups";
    }

    /**
     * Chat ID picker assist (PRD US-7): hidden entirely if no bot token is set. A getUpdates
     * failure (rate limit/network) degrades to an empty list rather than breaking the form --
     * manual chatId entry must keep working regardless.
     */
    private void addChatCandidates(Model model) {
        boolean tokenConfigured = telegramBotConfigService.currentToken().isPresent();
        model.addAttribute("tokenConfigured", tokenConfigured);
        if (!tokenConfigured) {
            return;
        }

        List<String> registeredChatIds = telegramGroupService.findAll().stream()
                .map(TelegramGroup::getChatId)
                .toList();
        List<TelegramChatCandidate> candidates;
        try {
            candidates = telegramGateway.fetchRecentChats();
        } catch (TelegramSendException e) {
            candidates = List.of();
        }
        model.addAttribute("chatCandidates", candidates.stream()
                .filter(candidate -> !registeredChatIds.contains(candidate.chatId()))
                .toList());
    }
}
