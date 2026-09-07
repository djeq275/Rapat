package id.jagr.rapat.telegram;

/**
 * A group/supergroup chat the bot has recently seen a message from, offered as a Chat ID
 * pick in the admin form -- not a persisted entity, see {@link TelegramGateway#fetchRecentChats()}.
 */
public record TelegramChatCandidate(String chatId, String displayName) {
}
