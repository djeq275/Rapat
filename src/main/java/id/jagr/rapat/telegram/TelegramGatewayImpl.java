package id.jagr.rapat.telegram;

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
                throw new TelegramSendException("Telegram menolak pesan: " + describe(response));
            }
        } catch (RestClientResponseException e) {
            TelegramApiResponse body = e.getResponseBodyAs(TelegramApiResponse.class);
            throw new TelegramSendException("Telegram menolak pesan: " + describe(body), e);
        } catch (RestClientException e) {
            throw new TelegramSendException("Gagal mengirim pesan ke Telegram: " + e.getMessage(), e);
        }
    }

    private String describe(TelegramApiResponse response) {
        return response != null && response.description() != null ? response.description() : "tidak ada keterangan dari Telegram";
    }

    private record SendMessageRequest(@JsonProperty("chat_id") String chatId, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramApiResponse(boolean ok, String description) {
    }
}
