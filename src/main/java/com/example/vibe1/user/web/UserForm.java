package com.example.vibe1.user.web;

import com.example.vibe1.user.Role;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserForm {

    private String email;
    private String fullName;
    private Role role;
    private Long divisionId;
    /** Blank means "keep existing password" on update, or "Google-login only" on create. */
    private String password;
    private boolean enabled = true;
    private boolean divisionLeader;
}
