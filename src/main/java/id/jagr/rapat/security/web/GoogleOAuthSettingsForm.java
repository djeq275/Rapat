package id.jagr.rapat.security.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleOAuthSettingsForm {

    private String clientId;

    /** Blank means "keep the existing secret unchanged" -- same convention as UserForm.password. */
    private String clientSecret;
}
