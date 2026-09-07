package id.jagr.rapat.telegram;

import java.util.List;

/** Thin seam over the Telegram Bot API so callers don't deal with HTTP directly. */
public interface TelegramGateway {

    /** @throws TelegramSendException if the token isn't set, the HTTP call fails, or Telegram rejects the message. */
    void sendMessage(String chatId, String text);

    /**
     * Groups/supergroups the bot has recently seen a message from, deduplicated by chat id --
     * a best-effort assist for picking a Chat ID, not a permanent source of truth (see
     * {@link TelegramChatCandidate}).
     *
     * @throws TelegramSendException if the token isn't set, the HTTP call fails, or Telegram rejects the request.
     */
    List<TelegramChatCandidate> fetchRecentChats();
}
