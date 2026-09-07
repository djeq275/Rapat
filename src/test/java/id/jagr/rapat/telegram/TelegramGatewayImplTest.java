package id.jagr.rapat.telegram;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TelegramGatewayImplTest {

    MockRestServiceServer server;
    TelegramBotConfigService botConfigService;
    TelegramGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        botConfigService = mock(TelegramBotConfigService.class);
        gateway = new TelegramGatewayImpl(builder, botConfigService);
    }

    @Test
    void sendsMessageSuccessfully() {
        when(botConfigService.currentToken()).thenReturn(Optional.of("test-token"));
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{}}", MediaType.APPLICATION_JSON));

        gateway.sendMessage("-100123", "halo");

        server.verify();
    }

    @Test
    void throwsWhenTokenNotConfigured() {
        when(botConfigService.currentToken()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gateway.sendMessage("-100123", "halo"))
                .isInstanceOf(TelegramSendException.class)
                .hasMessageContaining("belum diset");
    }

    @Test
    void throwsWithTelegramsDescriptionWhenRejected() {
        when(botConfigService.currentToken()).thenReturn(Optional.of("test-token"));
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"description\":\"chat not found\"}"));

        assertThatThrownBy(() -> gateway.sendMessage("-100123", "halo"))
                .isInstanceOf(TelegramSendException.class)
                .hasMessageContaining("chat not found");
    }

    @Test
    void fetchesDistinctGroupChatsIgnoringPrivateChatsAndDuplicates() {
        when(botConfigService.currentToken()).thenReturn(Optional.of("test-token"));
        server.expect(requestTo("https://api.telegram.org/bottest-token/getUpdates"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"ok":true,"result":[
                          {"update_id":1,"message":{"chat":{"id":-100111,"title":"Grup A","type":"supergroup"}}},
                          {"update_id":2,"message":{"chat":{"id":-100111,"title":"Grup A","type":"supergroup"}}},
                          {"update_id":3,"message":{"chat":{"id":222,"first_name":"Budi","type":"private"}}},
                          {"update_id":4,"message":{"chat":{"id":-100333,"title":"Grup B","type":"group"}}}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<TelegramChatCandidate> candidates = gateway.fetchRecentChats();

        assertThat(candidates).containsExactly(
                new TelegramChatCandidate("-100111", "Grup A"),
                new TelegramChatCandidate("-100333", "Grup B"));
        server.verify();
    }

    @Test
    void fetchRecentChatsThrowsWhenTokenNotConfigured() {
        when(botConfigService.currentToken()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gateway.fetchRecentChats())
                .isInstanceOf(TelegramSendException.class)
                .hasMessageContaining("belum diset");
    }

    @Test
    void fetchRecentChatsThrowsWithTelegramsDescriptionWhenRejected() {
        when(botConfigService.currentToken()).thenReturn(Optional.of("test-token"));
        server.expect(requestTo("https://api.telegram.org/bottest-token/getUpdates"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"description\":\"Unauthorized\"}"));

        assertThatThrownBy(() -> gateway.fetchRecentChats())
                .isInstanceOf(TelegramSendException.class)
                .hasMessageContaining("Unauthorized");
    }
}
