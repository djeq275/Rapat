package id.jagr.rapat.division.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import id.jagr.rapat.division.Division;
import id.jagr.rapat.division.DivisionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/divisions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DivisionAdminController {

    private final DivisionService divisionService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("divisions", divisionService.findAll());
        return "admin/division-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new DivisionForm());
        return "admin/division-form";
    }

    @PostMapping
    public String create(@ModelAttribute("form") @Validated DivisionForm form, BindingResult bindingResult) {
        if (!bindingResult.hasErrors()) {
            try {
                divisionService.create(form.getName());
            } catch (IllegalArgumentException ex) {
                bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            }
        }
        return bindingResult.hasErrors() ? "admin/division-form" : "redirect:/admin/divisions";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Division division = divisionService.findById(id);
        DivisionForm form = new DivisionForm();
        form.setName(division.getName());
        model.addAttribute("form", form);
        model.addAttribute("divisionId", id);
        return "admin/division-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("form") @Validated DivisionForm form,
                          BindingResult bindingResult, Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                divisionService.rename(id, form.getName());
            } catch (IllegalArgumentException ex) {
                bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("divisionId", id);
            return "admin/division-form";
        }
        return "redirect:/admin/divisions";
    }
}
