package com.example.vibe1.telegram.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.vibe1.division.Division;
import com.example.vibe1.division.DivisionRepository;
import com.example.vibe1.telegram.DivisionTelegramGroupService;
import com.example.vibe1.telegram.TelegramGroupService;

import lombok.RequiredArgsConstructor;

/**
 * Lives in the telegram module (not division.web) even though its route is
 * nested under /admin/divisions/** -- division must stay a dependency-free
 * leaf module (telegram -> division already exists via DivisionTelegramGroup,
 * so division -> telegram would close a 2-module cycle).
 */
@Controller
@RequestMapping("/admin/divisions/{divisionId}/telegram-groups")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DivisionTelegramGroupAdminController {

    private final DivisionRepository divisionRepository;
    private final TelegramGroupService telegramGroupService;
    private final DivisionTelegramGroupService divisionTelegramGroupService;

    @GetMapping
    public String edit(@PathVariable Long divisionId, Model model) {
        Division division = divisionRepository.findById(divisionId)
                .orElseThrow(() -> new IllegalArgumentException("Divisi tidak ditemukan"));
        model.addAttribute("division", division);
        model.addAttribute("groups", telegramGroupService.findActive());
        model.addAttribute("favoriteIds", divisionTelegramGroupService.findFavoriteGroupIds(divisionId));
        return "admin/division-telegram-groups";
    }

    @PostMapping
    public String update(@PathVariable Long divisionId,
                          @RequestParam(name = "telegramGroupIds", required = false) List<Long> telegramGroupIds) {
        divisionTelegramGroupService.replaceFavorites(divisionId, telegramGroupIds == null ? List.of() : telegramGroupIds);
        return "redirect:/admin/divisions";
    }
}
