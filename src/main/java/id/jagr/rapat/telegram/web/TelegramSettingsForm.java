package id.jagr.rapat.telegram.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TelegramSettingsForm {

    /** Blank means "keep the existing token unchanged" -- same convention as UserForm.password. */
    private String token;
}
