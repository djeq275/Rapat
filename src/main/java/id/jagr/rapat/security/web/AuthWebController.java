package id.jagr.rapat.security.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import id.jagr.rapat.security.KeycloakConfigService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthWebController {

    private final KeycloakConfigService keycloakConfigService;

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("keycloakConfigured", keycloakConfigService.isConfigured());
        return "auth/login";
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
