package id.jagr.rapat.telegram;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
class TelegramGatewayImpl implements TelegramGateway {

    private final RestClient restClient;
    private final TelegramBotConfigService botConfigService;

    TelegramGatewayImpl(RestClient.Builder restClientBuilder, TelegramBotConfigService botConfigService) {
        this.restClient = restClientBuilder.baseUrl("https://api.telegram.org").build();
        this.botConfigService = botConfigService;
    }

    @Override
    public void sendMessage(String chatId, String text) {
        String token = botConfigService.currentToken()
                .orElseThrow(() -> new TelegramSendException("Token bot Telegram belum diset oleh Admin"));

        try {
            TelegramApiResponse response = restClient.post()
                    .uri("/bot{token}/sendMessage", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SendMessageRequest(chatId, text))
                    .retrieve()
                    .body(TelegramApiResponse.class);

            if (response == null || !response.ok()) {
                throw new TelegramSendException("Telegram menolak pesan: " + describe(response == null ? null : response.description()));
            }
        } catch (RestClientResponseException e) {
            TelegramApiResponse body = e.getResponseBodyAs(TelegramApiResponse.class);
            throw new TelegramSendException("Telegram menolak pesan: " + describe(body == null ? null : body.description()), e);
        } catch (RestClientException e) {
            throw new TelegramSendException("Gagal mengirim pesan ke Telegram: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TelegramChatCandidate> fetchRecentChats() {
        String token = botConfigService.currentToken()
                .orElseThrow(() -> new TelegramSendException("Token bot Telegram belum diset oleh Admin"));

        try {
            GetUpdatesResponse response = restClient.get()
                    .uri("/bot{token}/getUpdates", token)
                    .retrieve()
                    .body(GetUpdatesResponse.class);

            if (response == null || !response.ok()) {
                throw new TelegramSendException("Telegram menolak permintaan getUpdates: " + describe(response == null ? null : response.description()));
            }
            return distinctGroupChats(response.result());
        } catch (RestClientResponseException e) {
            GetUpdatesResponse body = e.getResponseBodyAs(GetUpdatesResponse.class);
            throw new TelegramSendException("Telegram menolak permintaan getUpdates: " + describe(body == null ? null : body.description()), e);
        } catch (RestClientException e) {
            throw new TelegramSendException("Gagal mengambil daftar chat dari Telegram: " + e.getMessage(), e);
        }
    }

    private List<TelegramChatCandidate> distinctGroupChats(List<TelegramUpdate> updates) {
        Map<String, String> chatIdToDisplayName = new LinkedHashMap<>();
        if (updates != null) {
            for (TelegramUpdate update : updates) {
                TelegramChat chat = update.message() != null ? update.message().chat() : null;
                if (chat != null && ("group".equals(chat.type()) || "supergroup".equals(chat.type()))) {
                    chatIdToDisplayName.put(String.valueOf(chat.id()), chat.title());
                }
            }
        }
        return chatIdToDisplayName.entrySet().stream()
                .map(entry -> new TelegramChatCandidate(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String describe(String description) {
        return description != null ? description : "tidak ada keterangan dari Telegram";
    }

    private record SendMessageRequest(@JsonProperty("chat_id") String chatId, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramApiResponse(boolean ok, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GetUpdatesResponse(boolean ok, String description, List<TelegramUpdate> result) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramUpdate(TelegramMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramMessage(TelegramChat chat) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramChat(long id, String title, String type) {
    }
}
