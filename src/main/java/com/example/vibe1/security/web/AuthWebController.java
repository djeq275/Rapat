package com.example.vibe1.security.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthWebController {

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
