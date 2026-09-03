package com.example.vibe1.security;

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
class GoogleOAuthConfigServiceTest {

    @Mock
    GoogleOAuthConfigRepository repository;
    @Mock
    ApplicationEventPublisher eventPublisher;

    GoogleOAuthConfigService service;

    @Test
    void firstSaveRequiresClientSecret() {
        service = new GoogleOAuthConfigService(repository, eventPublisher);
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save("rapat-app", ""))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void firstSaveWithSecretCreatesRow() {
        service = new GoogleOAuthConfigService(repository, eventPublisher);
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.save("rapat-app", "s3cr3t");

        verify(eventPublisher).publishEvent(any(GoogleOAuthConfigSavedEvent.class));

        ArgumentCaptor<GoogleOAuthConfig> captor = ArgumentCaptor.forClass(GoogleOAuthConfig.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getClientId()).isEqualTo("rapat-app");
        assertThat(captor.getValue().getClientSecretEnc()).isEqualTo("s3cr3t");
    }

    @Test
    void blankSecretOnUpdateKeepsExistingSecretButUpdatesClientId() {
        service = new GoogleOAuthConfigService(repository, eventPublisher);
        GoogleOAuthConfig existing = new GoogleOAuthConfig();
        existing.setId(1L);
        existing.setClientId("old-client");
        existing.setClientSecretEnc("existing-secret");
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.save("new-client", "");

        ArgumentCaptor<GoogleOAuthConfig> captor = ArgumentCaptor.forClass(GoogleOAuthConfig.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getClientId()).isEqualTo("new-client");
        assertThat(captor.getValue().getClientSecretEnc()).isEqualTo("existing-secret");
    }

    @Test
    void nonBlankSecretOnUpdateReplacesSecret() {
        service = new GoogleOAuthConfigService(repository, eventPublisher);
        GoogleOAuthConfig existing = new GoogleOAuthConfig();
        existing.setId(1L);
        existing.setClientSecretEnc("old-secret");
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.save("rapat-app", "new-secret");

        ArgumentCaptor<GoogleOAuthConfig> captor = ArgumentCaptor.forClass(GoogleOAuthConfig.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getClientSecretEnc()).isEqualTo("new-secret");
    }

    @Test
    void isConfiguredReflectsRepositoryState() {
        service = new GoogleOAuthConfigService(repository, eventPublisher);
        lenient().when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        assertThat(service.isConfigured()).isFalse();

        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(new GoogleOAuthConfig()));

        assertThat(service.isConfigured()).isTrue();
        assertThatCode(() -> service.currentConfig()).doesNotThrowAnyException();
    }
}
