package com.example.vibe1.security.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KeycloakSettingsForm {

    private String serverUrl;
    private String realm;
    private String clientId;

    /** Blank means "keep the existing secret unchanged" -- same convention as UserForm.password. */
    private String clientSecret;
}
