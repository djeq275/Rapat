package id.jagr.rapat.user.web;

import id.jagr.rapat.user.Role;

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
