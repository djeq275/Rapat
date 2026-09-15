package id.jagr.rapat.user.web;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import id.jagr.rapat.user.AppRole;
import id.jagr.rapat.user.RoleService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RoleService roleService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("roles", roleService.findAll());
        return "admin/role-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new RoleForm());
        addReferenceData(model, null);
        return "admin/role-form";
    }

    @PostMapping
    public String create(@ModelAttribute("form") @Validated RoleForm form, BindingResult bindingResult, Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                roleService.create(form);
            } catch (IllegalArgumentException ex) {
                bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            }
        }
        if (bindingResult.hasErrors()) {
            addReferenceData(model, null);
            return "admin/role-form";
        }
        return "redirect:/admin/roles";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        AppRole role = roleService.findById(id);
        RoleForm form = new RoleForm();
        form.setName(role.getName());
        form.setRequiresDivision(role.isRequiresDivision());
        form.setAutoInviteToAllMeetings(role.isAutoInviteToAllMeetings());
        form.setCanViewAllDivisions(role.isCanViewAllDivisions());
        form.setCanOrganizeMeetings(role.isCanOrganizeMeetings());
        form.setCapabilityIds(role.getCapabilities().stream().map(c -> c.getId()).toList());
        model.addAttribute("form", form);
        model.addAttribute("roleId", id);
        model.addAttribute("builtIn", role.isBuiltIn());
        addReferenceData(model, id);
        return "admin/role-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("form") @Validated RoleForm form,
                          BindingResult bindingResult, Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                roleService.update(id, form);
            } catch (IllegalArgumentException ex) {
                bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("roleId", id);
            model.addAttribute("builtIn", roleService.findById(id).isBuiltIn());
            addReferenceData(model, id);
            return "admin/role-form";
        }
        return "redirect:/admin/roles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            roleService.delete(id);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }

    private void addReferenceData(Model model, Long roleId) {
        model.addAttribute("capabilities", roleService.findAllCapabilities());
        if (roleId != null) {
            model.addAttribute("userCount", roleService.countUsersWithRole(roleId));
        }
    }
}
