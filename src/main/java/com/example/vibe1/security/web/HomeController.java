package com.example.vibe1.security.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.vibe1.security.UserPrincipal;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("principal", principal);
        return "home";
    }
}
