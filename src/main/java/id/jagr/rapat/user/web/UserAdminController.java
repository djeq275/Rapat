package id.jagr.rapat.user.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import id.jagr.rapat.division.DivisionRepository;
import id.jagr.rapat.user.Role;
import id.jagr.rapat.user.User;
import id.jagr.rapat.user.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;
    private final DivisionRepository divisionRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/user-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new UserForm());
        addReferenceData(model);
        return "admin/user-form";
    }

    @PostMapping
    public String create(@ModelAttribute("form") UserForm form, BindingResult bindingResult, Model model) {
        try {
            userService.create(form);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("error", ex.getMessage());
        }
        if (bindingResult.hasErrors()) {
            addReferenceData(model);
            return "admin/user-form";
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userService.findByIdWithDivision(id);
        UserForm form = new UserForm();
        form.setEmail(user.getEmail());
        form.setFullName(user.getFullName());
        form.setRole(user.getRole());
        form.setEnabled(user.isEnabled());
        if (user.getDivision() != null) {
            form.setDivisionId(user.getDivision().getId());
            form.setDivisionLeader(id.equals(user.getDivision().getKetuaDivisiUserId()));
        }
        model.addAttribute("form", form);
        model.addAttribute("userId", id);
        addReferenceData(model);
        return "admin/user-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("form") UserForm form,
                          BindingResult bindingResult, Model model) {
        try {
            userService.update(id, form);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("error", ex.getMessage());
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", id);
            addReferenceData(model);
            return "admin/user-form";
        }
        return "redirect:/admin/users";
    }

    private void addReferenceData(Model model) {
        model.addAttribute("divisions", divisionRepository.findAll());
        model.addAttribute("roles", Role.values());
    }
}
