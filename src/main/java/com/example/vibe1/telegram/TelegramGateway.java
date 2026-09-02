package com.example.vibe1.telegram;

/** Thin seam over the Telegram Bot API so callers don't deal with HTTP directly. */
public interface TelegramGateway {

    /** @throws TelegramSendException if the token isn't set, the HTTP call fails, or Telegram rejects the message. */
    void sendMessage(String chatId, String text);
}
