package id.jagr.rapat.telegram.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TelegramGroupForm {

    private String name;
    private String chatId;
    private boolean enabled = true;
}
