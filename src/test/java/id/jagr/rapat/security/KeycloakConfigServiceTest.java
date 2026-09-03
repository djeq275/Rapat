package id.jagr.rapat.security;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakConfigServiceTest {

    @Mock
    KeycloakConfigRepository repository;
    @Mock
    ApplicationEventPublisher eventPublisher;

    KeycloakConfigService service;

    @Test
    void firstSaveRequiresClientSecret() {
        service = new KeycloakConfigService(repository, eventPublisher);
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save("https://keycloak.local", "company", "rapat-app", ""))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void firstSaveWithSecretCreatesRow() {
        service = new KeycloakConfigService(repository, eventPublisher);
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.save("https://keycloak.local", "company", "rapat-app", "s3cr3t");

        verify(eventPublisher).publishEvent(any(KeycloakConfigSavedEvent.class));

        ArgumentCaptor<KeycloakConfig> captor = ArgumentCaptor.forClass(KeycloakConfig.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getServerUrl()).isEqualTo("https://keycloak.local");
        assertThat(captor.getValue().getRealm()).isEqualTo("company");
        assertThat(captor.getValue().getClientId()).isEqualTo("rapat-app");
        assertThat(captor.getValue().getClientSecretEnc()).isEqualTo("s3cr3t");
    }

    @Test
    void blankSecretOnUpdateKeepsExistingSecretButUpdatesOtherFields() {
        service = new KeycloakConfigService(repository, eventPublisher);
        KeycloakConfig existing = new KeycloakConfig();
        existing.setId(1L);
        existing.setServerUrl("https://old.local");
        existing.setRealm("old-realm");
        existing.setClientId("old-client");
        existing.setClientSecretEnc("existing-secret");
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.save("https://new.local", "new-realm", "new-client", "");

        ArgumentCaptor<KeycloakConfig> captor = ArgumentCaptor.forClass(KeycloakConfig.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getServerUrl()).isEqualTo("https://new.local");
        assertThat(captor.getValue().getRealm()).isEqualTo("new-realm");
        assertThat(captor.getValue().getClientId()).isEqualTo("new-client");
        assertThat(captor.getValue().getClientSecretEnc()).isEqualTo("existing-secret");
    }

    @Test
    void nonBlankSecretOnUpdateReplacesSecret() {
        service = new KeycloakConfigService(repository, eventPublisher);
        KeycloakConfig existing = new KeycloakConfig();
        existing.setId(1L);
        existing.setClientSecretEnc("old-secret");
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.save("https://keycloak.local", "company", "rapat-app", "new-secret");

        ArgumentCaptor<KeycloakConfig> captor = ArgumentCaptor.forClass(KeycloakConfig.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getClientSecretEnc()).isEqualTo("new-secret");
    }

    @Test
    void isConfiguredReflectsRepositoryState() {
        service = new KeycloakConfigService(repository, eventPublisher);
        lenient().when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        assertThat(service.isConfigured()).isFalse();

        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(new KeycloakConfig()));

        assertThat(service.isConfigured()).isTrue();
        assertThatCode(() -> service.currentConfig()).doesNotThrowAnyException();
    }
}
